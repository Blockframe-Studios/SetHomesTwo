#!/usr/bin/env python3
"""Compute and apply a release from the changeset files in .changeset/.

Split into `plan` (compute, touch nothing) and `apply` (edit the README,
delete consumed changesets) so the release workflow's dry-run can call
`plan` and be guaranteed to write nothing.
"""

import argparse
import glob
import json
import os
import re
import sys

BUMPS = ("patch", "minor", "major")
RANK = {"patch": 0, "minor": 1, "major": 2}
CHANGELOG_HEADING = "### Changelog\n"


def parse_changeset(text):
    match = re.match(r"^---\s*\nbump:\s*(\S+)\s*\n---\s*\n(.*)$", text, re.DOTALL)
    if not match:
        raise ValueError("changeset must start with '---', a 'bump:' line, then '---'")

    bump = match.group(1).strip()
    if bump not in BUMPS:
        raise ValueError("bump must be one of %s, got '%s'" % (", ".join(BUMPS), bump))

    summary = " ".join(line.strip() for line in match.group(2).split("\n") if line.strip())
    if not summary:
        raise ValueError("changeset has no summary text")

    return bump, summary


def highest_bump(bumps):
    return max(bumps, key=lambda b: RANK[b])


def next_version(current, bump):
    parts = current.split(".")
    if len(parts) != 3 or not all(p.isdigit() for p in parts):
        raise ValueError("version must be MAJOR.MINOR.PATCH of digits, got '%s'" % current)

    major, minor, patch = (int(p) for p in parts)
    if bump == "major":
        return "%d.0.0" % (major + 1)
    if bump == "minor":
        return "%d.%d.0" % (major, minor + 1)
    return "%d.%d.%d" % (major, minor, patch + 1)


def render_section(version, date, entries):
    lines = ["#### %s (%s)" % (version, date), ""]
    lines += ["- %s" % e for e in entries]
    return "\n".join(lines) + "\n"


def insert_changelog(readme_text, section):
    index = readme_text.find(CHANGELOG_HEADING)
    if index == -1:
        raise ValueError("README has no '### Changelog' heading")

    cut = index + len(CHANGELOG_HEADING)
    rest = readme_text[cut:].lstrip("\n")
    return readme_text[:cut] + "\n" + section + "\n" + rest


def collect(changeset_dir):
    """Return (paths, bumps, entries) for every changeset, skipping README.md."""
    paths, bumps, entries = [], [], []
    for path in sorted(glob.glob(os.path.join(changeset_dir, "*.md"))):
        if os.path.basename(path).lower() == "readme.md":
            continue
        with open(path, encoding="utf-8") as handle:
            try:
                bump, summary = parse_changeset(handle.read())
            except ValueError as error:
                raise ValueError("%s: %s" % (path, error))
        paths.append(path)
        bumps.append(bump)
        entries.append(summary)
    return paths, bumps, entries


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("plan", "apply"))
    parser.add_argument("--current-version", required=True)
    parser.add_argument("--changeset-dir", default=".changeset")
    parser.add_argument("--readme", default="README.md")
    parser.add_argument("--date")
    args = parser.parse_args(argv)

    try:
        paths, bumps, entries = collect(args.changeset_dir)
    except ValueError as error:
        sys.stderr.write("%s\n" % error)
        return 1

    if not paths:
        sys.stderr.write("no changesets in %s - nothing to release\n" % args.changeset_dir)
        return 3

    bump = highest_bump(bumps)
    result = {
        "bump": bump,
        "current": args.current_version,
        "next": next_version(args.current_version, bump),
        "entries": entries,
    }

    if args.command == "apply":
        if not args.date:
            sys.stderr.write("apply requires --date\n")
            return 1
        with open(args.readme, encoding="utf-8") as handle:
            readme = handle.read()
        section = render_section(result["next"], args.date, entries)
        with open(args.readme, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(insert_changelog(readme, section))
        for path in paths:
            os.remove(path)

    sys.stdout.write(json.dumps(result, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())

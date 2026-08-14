#!/usr/bin/env python3
"""Write a changeset file describing how this change affects the version.

Run with no arguments for prompts, or:
    python3 scripts/changeset.py minor "Summary of the change"
"""

import os
import random
import subprocess
import sys

BUMPS = ("patch", "minor", "major")

DESCRIPTIONS = {
    "patch": "bug fix, no behaviour change for existing setups",
    "minor": "new functionality, existing setups keep working",
    "major": "breaking - a command, permission, config key or\n"
             "               runtime requirement changes for existing servers",
}

ADJECTIVES = ["brave", "calm", "clever", "eager", "gentle", "happy", "kind",
              "lucky", "proud", "quiet", "swift", "tidy", "warm", "wise"]
ANIMALS = ["badgers", "cranes", "dolphins", "foxes", "herons", "lynxes",
           "otters", "pandas", "ravens", "seals", "tigers", "wolves"]
VERBS = ["arrive", "dance", "gather", "listen", "return", "shine", "sing",
         "smile", "travel", "wander", "wave", "wonder"]


def random_name():
    return "%s-%s-%s" % (random.choice(ADJECTIVES), random.choice(ANIMALS), random.choice(VERBS))


def prompt_bump():
    sys.stdout.write("\n  What kind of change is this?\n\n")
    for index, bump in enumerate(BUMPS, start=1):
        sys.stdout.write("    %d) %-7s %s\n" % (index, bump, DESCRIPTIONS[bump]))
    sys.stdout.write("\n")

    while True:
        choice = input("  > ").strip()
        if choice in ("1", "2", "3"):
            return BUMPS[int(choice) - 1]
        if choice in BUMPS:
            return choice
        sys.stdout.write("  Enter 1, 2 or 3.\n")


def prompt_summary():
    sys.stdout.write("\n  Summary (one line, written for server owners):\n")
    while True:
        summary = input("  > ").strip()
        if summary:
            return summary
        sys.stdout.write("  A summary is required.\n")


def write_changeset(bump, summary, changeset_dir):
    if not os.path.isdir(changeset_dir):
        os.makedirs(changeset_dir)

    path = os.path.join(changeset_dir, random_name() + ".md")
    while os.path.exists(path):
        path = os.path.join(changeset_dir, random_name() + ".md")

    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("---\nbump: %s\n---\n\n%s\n" % (bump, summary))

    return path


def main(argv):
    changeset_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".changeset")

    if len(argv) >= 2:
        bump, summary = argv[0], " ".join(argv[1:])
        if bump not in BUMPS:
            sys.stderr.write("bump must be one of: %s\n" % ", ".join(BUMPS))
            return 1
    elif argv:
        sys.stderr.write("usage: changeset.py [patch|minor|major \"summary\"]\n")
        return 1
    else:
        bump = prompt_bump()
        summary = prompt_summary()

    path = write_changeset(bump, summary, changeset_dir)
    relative = os.path.relpath(path, os.getcwd())

    try:
        staged = subprocess.call(["git", "add", path]) == 0
    except OSError:
        staged = False
    sys.stdout.write("\n  Created %s%s\n\n" % (relative, "  (staged)" if staged else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

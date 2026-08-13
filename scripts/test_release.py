import json
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import release


class ParseChangeset(unittest.TestCase):

    def test_parses_bump_and_summary(self):
        text = "---\nbump: minor\n---\n\nAdded an update checker\n"
        self.assertEqual(release.parse_changeset(text), ("minor", "Added an update checker"))

    def test_joins_a_multi_line_summary(self):
        text = "---\nbump: patch\n---\n\nFixed a thing\nthat spanned lines\n"
        self.assertEqual(
            release.parse_changeset(text),
            ("patch", "Fixed a thing that spanned lines"),
        )

    def test_rejects_missing_frontmatter(self):
        with self.assertRaises(ValueError):
            release.parse_changeset("just a summary with no frontmatter\n")

    def test_rejects_unknown_bump(self):
        with self.assertRaises(ValueError):
            release.parse_changeset("---\nbump: huge\n---\n\nSummary\n")

    def test_rejects_empty_summary(self):
        with self.assertRaises(ValueError):
            release.parse_changeset("---\nbump: patch\n---\n\n\n")


class HighestBump(unittest.TestCase):

    def test_major_beats_everything(self):
        self.assertEqual(release.highest_bump(["patch", "major", "minor"]), "major")

    def test_minor_beats_patch(self):
        self.assertEqual(release.highest_bump(["patch", "minor", "patch"]), "minor")

    def test_all_patch_is_patch(self):
        self.assertEqual(release.highest_bump(["patch", "patch"]), "patch")


class NextVersion(unittest.TestCase):

    def test_patch(self):
        self.assertEqual(release.next_version("1.2.0", "patch"), "1.2.1")

    def test_minor_resets_patch(self):
        self.assertEqual(release.next_version("1.2.3", "minor"), "1.3.0")

    def test_major_resets_minor_and_patch(self):
        self.assertEqual(release.next_version("1.2.3", "major"), "2.0.0")

    def test_rejects_non_numeric(self):
        with self.assertRaises(ValueError):
            release.next_version("1.2.0-SNAPSHOT", "patch")


class RenderAndInsert(unittest.TestCase):

    def test_renders_heading_and_entries(self):
        section = release.render_section("1.3.0", "2026-08-13", ["First thing", "Second thing"])
        self.assertEqual(
            section,
            "#### 1.3.0 (2026-08-13)\n\n- First thing\n- Second thing\n",
        )

    def test_inserts_directly_below_the_changelog_heading(self):
        readme = "### Changelog\n\n#### 1.2.0 (2026-08-12)\n\n- Older thing\n"
        section = release.render_section("1.3.0", "2026-08-13", ["New thing"])
        out = release.insert_changelog(readme, section)
        self.assertEqual(
            out,
            "### Changelog\n\n#### 1.3.0 (2026-08-13)\n\n- New thing\n\n"
            "#### 1.2.0 (2026-08-12)\n\n- Older thing\n",
        )

    def test_rejects_a_readme_with_no_changelog_heading(self):
        with self.assertRaises(ValueError):
            release.insert_changelog("# Readme\n\nNo changelog here\n", "#### 1.3.0 (x)\n")


class Cli(unittest.TestCase):

    def _run(self, args, cwd):
        script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "release.py")
        return subprocess.run(
            [sys.executable, script] + args,
            cwd=cwd, capture_output=True, text=True,
        )

    def test_plan_exits_3_when_no_changesets(self):
        with tempfile.TemporaryDirectory() as d:
            os.mkdir(os.path.join(d, ".changeset"))
            r = self._run(["plan", "--current-version", "1.2.0", "--changeset-dir", ".changeset"], d)
            self.assertEqual(r.returncode, 3)

    def test_plan_reports_the_highest_bump(self):
        with tempfile.TemporaryDirectory() as d:
            cs = os.path.join(d, ".changeset")
            os.mkdir(cs)
            with open(os.path.join(cs, "a.md"), "w") as f:
                f.write("---\nbump: patch\n---\n\nA patch thing\n")
            with open(os.path.join(cs, "b.md"), "w") as f:
                f.write("---\nbump: minor\n---\n\nA minor thing\n")
            r = self._run(["plan", "--current-version", "1.2.0", "--changeset-dir", ".changeset"], d)
            self.assertEqual(r.returncode, 0, r.stderr)
            out = json.loads(r.stdout)
            self.assertEqual(out["bump"], "minor")
            self.assertEqual(out["next"], "1.3.0")
            self.assertEqual(sorted(out["entries"]), ["A minor thing", "A patch thing"])

    def test_apply_edits_the_readme_and_deletes_changesets(self):
        with tempfile.TemporaryDirectory() as d:
            cs = os.path.join(d, ".changeset")
            os.mkdir(cs)
            with open(os.path.join(cs, "a.md"), "w") as f:
                f.write("---\nbump: minor\n---\n\nAdded a thing\n")
            with open(os.path.join(cs, "README.md"), "w") as f:
                f.write("how to write a changeset\n")
            readme = os.path.join(d, "README.md")
            with open(readme, "w") as f:
                f.write("### Changelog\n\n#### 1.2.0 (2026-08-12)\n\n- Older\n")

            r = self._run(
                ["apply", "--current-version", "1.2.0", "--changeset-dir", ".changeset",
                 "--readme", "README.md", "--date", "2026-08-13"], d)
            self.assertEqual(r.returncode, 0, r.stderr)

            with open(readme) as f:
                text = f.read()
            self.assertIn("#### 1.3.0 (2026-08-13)", text)
            self.assertIn("- Added a thing", text)
            self.assertIn("#### 1.2.0 (2026-08-12)", text)

            self.assertFalse(os.path.exists(os.path.join(cs, "a.md")))
            self.assertTrue(os.path.exists(os.path.join(cs, "README.md")),
                            "the directory's own README must not be consumed")


if __name__ == "__main__":
    unittest.main()

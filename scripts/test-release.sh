#!/usr/bin/env bash
# Bash port of scripts/test_release.py - defines the release.sh contract.
#
# Sources release.sh directly so the unit-level tests (parse_changeset,
# highest_bump, next_version, render_section, insert_changelog) can call the
# functions in-process, the same way the Python suite imported release.py.
# The Cli-equivalent tests below shell out to `bash scripts/release.sh ...`
# the same way the Python suite used subprocess.
#
# Run with: bash scripts/test-release.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_SH="$SCRIPT_DIR/release.sh"

# shellcheck source=./release.sh
source "$RELEASE_SH"

PASS=0
FAIL=0

pass() {
  PASS=$((PASS + 1))
  printf 'ok   - %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf 'FAIL - %s\n' "$1"
  if [ -n "${2:-}" ]; then
    printf '       %s\n' "$2"
  fi
}

assert_eq() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    pass "$desc"
  else
    fail "$desc" "expected [$expected] got [$actual]"
  fi
}

assert_status() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    pass "$desc"
  else
    fail "$desc" "expected exit $expected got $actual"
  fi
}

assert_contains() {
  local desc="$1" haystack="$2" needle="$3"
  case "$haystack" in
    *"$needle"*) pass "$desc" ;;
    *) fail "$desc" "expected to find [$needle]" ;;
  esac
}

# mktemp -d gives a Windows-style path under Git Bash; release.sh and the
# tests below only ever use it through bash builtins/coreutils, which handle
# it fine.
new_tmpdir() {
  mktemp -d "${TMPDIR:-/tmp}/release-sh-test.XXXXXX"
}

### ParseChangeset ###########################################################

test_parses_bump_and_summary() {
  local text=$'---\nbump: minor\n---\n\nAdded an update checker\n'
  if parse_changeset "$text"; then
    assert_eq "parse_changeset: parses bump and summary (bump)" "minor" "$PARSED_BUMP"
    assert_eq "parse_changeset: parses bump and summary (summary)" "Added an update checker" "$PARSED_SUMMARY"
  else
    fail "parse_changeset: parses bump and summary" "unexpected failure: $PARSE_ERROR"
  fi
}

test_joins_a_multi_line_summary() {
  local text=$'---\nbump: patch\n---\n\nFixed a thing\nthat spanned lines\n'
  if parse_changeset "$text"; then
    assert_eq "parse_changeset: joins a multi-line summary (bump)" "patch" "$PARSED_BUMP"
    assert_eq "parse_changeset: joins a multi-line summary (summary)" "Fixed a thing that spanned lines" "$PARSED_SUMMARY"
  else
    fail "parse_changeset: joins a multi-line summary" "unexpected failure: $PARSE_ERROR"
  fi
}

test_rejects_missing_frontmatter() {
  local text="just a summary with no frontmatter"$'\n'
  if parse_changeset "$text"; then
    fail "parse_changeset: rejects missing frontmatter" "expected failure, got bump=$PARSED_BUMP"
  else
    pass "parse_changeset: rejects missing frontmatter"
  fi
}

test_rejects_unknown_bump() {
  local text=$'---\nbump: huge\n---\n\nSummary\n'
  if parse_changeset "$text"; then
    fail "parse_changeset: rejects unknown bump" "expected failure, got bump=$PARSED_BUMP"
  else
    pass "parse_changeset: rejects unknown bump"
  fi
}

test_rejects_empty_summary() {
  local text=$'---\nbump: patch\n---\n\n\n'
  if parse_changeset "$text"; then
    fail "parse_changeset: rejects empty summary" "expected failure, got summary=[$PARSED_SUMMARY]"
  else
    pass "parse_changeset: rejects empty summary"
  fi
}

### HighestBump ###############################################################

test_major_beats_everything() {
  highest_bump patch major minor
  assert_eq "highest_bump: major beats everything" "major" "$HIGHEST_BUMP"
}

test_minor_beats_patch() {
  highest_bump patch minor patch
  assert_eq "highest_bump: minor beats patch" "minor" "$HIGHEST_BUMP"
}

test_all_patch_is_patch() {
  highest_bump patch patch
  assert_eq "highest_bump: all patch is patch" "patch" "$HIGHEST_BUMP"
}

### NextVersion ################################################################

test_next_version_patch() {
  next_version "1.2.0" "patch"
  assert_eq "next_version: patch" "1.2.1" "$NEXT_VERSION"
}

test_next_version_minor_resets_patch() {
  next_version "1.2.3" "minor"
  assert_eq "next_version: minor resets patch" "1.3.0" "$NEXT_VERSION"
}

test_next_version_major_resets_minor_and_patch() {
  next_version "1.2.3" "major"
  assert_eq "next_version: major resets minor and patch" "2.0.0" "$NEXT_VERSION"
}

test_next_version_rejects_non_numeric() {
  if next_version "1.2.0-SNAPSHOT" "patch"; then
    fail "next_version: rejects non-numeric" "expected failure, got $NEXT_VERSION"
  else
    pass "next_version: rejects non-numeric"
  fi
}

### RenderAndInsert #############################################################

test_renders_heading_and_entries() {
  render_section "1.3.0" "2026-08-13" "First thing" "Second thing"
  assert_eq "render_section: heading and entries" \
    $'#### 1.3.0 (2026-08-13)\n\n- First thing\n- Second thing\n' \
    "$RENDERED_SECTION"
}

test_inserts_directly_below_the_changelog_heading() {
  local readme=$'### Changelog\n\n#### 1.2.0 (2026-08-12)\n\n- Older thing\n'
  render_section "1.3.0" "2026-08-13" "New thing"
  local section="$RENDERED_SECTION"
  if insert_changelog "$readme" "$section"; then
    assert_eq "insert_changelog: inserts directly below heading" \
      $'### Changelog\n\n#### 1.3.0 (2026-08-13)\n\n- New thing\n\n#### 1.2.0 (2026-08-12)\n\n- Older thing\n' \
      "$INSERTED_TEXT"
  else
    fail "insert_changelog: inserts directly below heading" "unexpected failure: $INSERT_ERROR"
  fi
}

test_rejects_a_readme_with_no_changelog_heading() {
  if insert_changelog $'# Readme\n\nNo changelog here\n' $'#### 1.3.0 (x)\n'; then
    fail "insert_changelog: rejects readme with no heading" "expected failure"
  else
    pass "insert_changelog: rejects readme with no heading"
  fi
}

### Cli (subprocess, like the Python suite) ####################################

test_plan_exits_3_when_no_changesets() {
  local d status
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  ( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0 --changeset-dir .changeset >/dev/null 2>&1 )
  status=$?
  assert_status "cli: plan exits 3 when no changesets" "3" "$status"
  rm -rf "$d"
}

test_plan_reports_the_highest_bump() {
  local d out status
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: patch\n---\n\nA patch thing\n' > "$d/.changeset/a.md"
  printf -- '---\nbump: minor\n---\n\nA minor thing\n' > "$d/.changeset/b.md"
  out="$( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0 --changeset-dir .changeset )"
  status=$?
  assert_status "cli: plan reports the highest bump (exit)" "0" "$status"
  assert_contains "cli: plan reports the highest bump (bump)" "$out" "bump=minor"
  assert_contains "cli: plan reports the highest bump (next)" "$out" "next=1.3.0"
  rm -rf "$d"
}

test_apply_edits_the_readme_and_deletes_changesets() {
  local d status readme_text
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: minor\n---\n\nAdded a thing\n' > "$d/.changeset/a.md"
  printf -- 'how to write a changeset\n' > "$d/.changeset/README.md"
  printf -- '### Changelog\n\n#### 1.2.0 (2026-08-12)\n\n- Older\n' > "$d/README.md"

  ( cd "$d" && bash "$RELEASE_SH" apply --current-version 1.2.0 --changeset-dir .changeset \
      --readme README.md --date 2026-08-13 >/dev/null )
  status=$?
  assert_status "cli: apply exits 0" "0" "$status"

  readme_text="$(cat "$d/README.md")"
  assert_contains "cli: apply inserts the next heading" "$readme_text" "#### 1.3.0 (2026-08-13)"
  assert_contains "cli: apply inserts the new entry" "$readme_text" "- Added a thing"
  assert_contains "cli: apply keeps the older heading" "$readme_text" "#### 1.2.0 (2026-08-12)"

  if [ -e "$d/.changeset/a.md" ]; then
    fail "cli: apply deletes the consumed changeset"
  else
    pass "cli: apply deletes the consumed changeset"
  fi
  if [ -e "$d/.changeset/README.md" ]; then
    pass "cli: apply does not consume the directory's own README"
  else
    fail "cli: apply does not consume the directory's own README"
  fi
  rm -rf "$d"
}

### Extra coverage beyond the Python suite ######################################
# (behaviours the task spec calls out explicitly: CRLF tolerance, README.md
#  skipped case-insensitively, version format rejected before arithmetic,
#  malformed changeset failing loudly at CLI level, plan/entries/notes never
#  writing anything.)

test_crlf_tolerance() {
  local text=$'---\r\nbump: minor\r\n---\r\n\r\nAdded an update checker\r\n'
  if parse_changeset "$text"; then
    assert_eq "parse_changeset: tolerates CRLF (bump)" "minor" "$PARSED_BUMP"
    assert_eq "parse_changeset: tolerates CRLF (summary)" "Added an update checker" "$PARSED_SUMMARY"
  else
    fail "parse_changeset: tolerates CRLF" "unexpected failure: $PARSE_ERROR"
  fi
}

test_readme_skipped_case_insensitively() {
  local d out status
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: patch\n---\n\nA patch thing\n' > "$d/.changeset/a.md"
  # Deliberately malformed - if collect() did not skip this case-insensitive
  # README, plan would fail with a parse error instead of succeeding.
  printf -- 'not a changeset at all\n' > "$d/.changeset/ReadMe.MD"
  out="$( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0 --changeset-dir .changeset )"
  status=$?
  assert_status "collect: skips README.md case-insensitively (exit)" "0" "$status"
  assert_contains "collect: skips README.md case-insensitively (bump)" "$out" "bump=patch"
  rm -rf "$d"
}

test_cli_rejects_bad_version_format_before_arithmetic() {
  local d status err
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: patch\n---\n\nA patch thing\n' > "$d/.changeset/a.md"
  err="$( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0-SNAPSHOT --changeset-dir .changeset 2>&1 >/dev/null )"
  status=$?
  assert_status "cli: rejects non-numeric version before arithmetic (exit)" "1" "$status"
  assert_contains "cli: rejects non-numeric version before arithmetic (message)" "$err" "1.2.0-SNAPSHOT"
  rm -rf "$d"
}

test_cli_malformed_changeset_exits_1() {
  local d status
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- 'no frontmatter here\n' > "$d/.changeset/a.md"
  ( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0 --changeset-dir .changeset >/dev/null 2>&1 )
  status=$?
  assert_status "cli: malformed changeset exits 1" "1" "$status"
  rm -rf "$d"
}

test_notes_extracts_entry_for_version_only() {
  local d out
  d="$(new_tmpdir)"
  printf -- '### Changelog\n\n#### 1.3.0 (2026-08-13)\n\n- New thing\n\n#### 1.2.0 (2026-08-12)\n\n- Older thing\n' > "$d/README.md"
  out="$( cd "$d" && bash "$RELEASE_SH" notes --readme README.md --version 1.3.0 )"
  assert_contains "notes: extracts only the requested version" "$out" "New thing"
  case "$out" in
    *"Older thing"*) fail "notes: does not leak the older entry" ;;
    *) pass "notes: does not leak the older entry" ;;
  esac
  case "$out" in
    *"1.3.0 ("*) fail "notes: omits the heading line" ;;
    *) pass "notes: omits the heading line" ;;
  esac
  rm -rf "$d"
}

test_entries_command_lists_all_summaries() {
  local d out
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: patch\n---\n\nA patch thing\n' > "$d/.changeset/a.md"
  printf -- '---\nbump: minor\n---\n\nA minor thing\n' > "$d/.changeset/b.md"
  out="$( cd "$d" && bash "$RELEASE_SH" entries --changeset-dir .changeset )"
  assert_contains "entries: lists first summary" "$out" "- A patch thing"
  assert_contains "entries: lists second summary" "$out" "- A minor thing"
  rm -rf "$d"
}

test_plan_entries_notes_never_mutate() {
  local d before after
  d="$(new_tmpdir)"
  mkdir -p "$d/.changeset"
  printf -- '---\nbump: minor\n---\n\nA minor thing\n' > "$d/.changeset/a.md"
  printf -- '### Changelog\n\n#### 1.2.0 (x)\n\n- Older\n' > "$d/README.md"
  before="$(cd "$d" && find . -type f | sort | xargs -I{} sh -c 'printf "%s " {}; cat {}')"
  ( cd "$d" && bash "$RELEASE_SH" plan --current-version 1.2.0 --changeset-dir .changeset >/dev/null 2>&1 )
  ( cd "$d" && bash "$RELEASE_SH" entries --changeset-dir .changeset >/dev/null 2>&1 )
  ( cd "$d" && bash "$RELEASE_SH" notes --readme README.md --version 1.2.0 >/dev/null 2>&1 )
  after="$(cd "$d" && find . -type f | sort | xargs -I{} sh -c 'printf "%s " {}; cat {}')"
  assert_eq "plan/entries/notes never write, delete or modify anything" "$before" "$after"
  rm -rf "$d"
}

### Run everything ##############################################################

test_parses_bump_and_summary
test_joins_a_multi_line_summary
test_rejects_missing_frontmatter
test_rejects_unknown_bump
test_rejects_empty_summary

test_major_beats_everything
test_minor_beats_patch
test_all_patch_is_patch

test_next_version_patch
test_next_version_minor_resets_patch
test_next_version_major_resets_minor_and_patch
test_next_version_rejects_non_numeric

test_renders_heading_and_entries
test_inserts_directly_below_the_changelog_heading
test_rejects_a_readme_with_no_changelog_heading

test_plan_exits_3_when_no_changesets
test_plan_reports_the_highest_bump
test_apply_edits_the_readme_and_deletes_changesets

test_crlf_tolerance
test_readme_skipped_case_insensitively
test_cli_rejects_bad_version_format_before_arithmetic
test_cli_malformed_changeset_exits_1
test_notes_extracts_entry_for_version_only
test_entries_command_lists_all_summaries
test_plan_entries_notes_never_mutate

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0

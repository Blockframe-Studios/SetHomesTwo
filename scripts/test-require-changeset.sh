#!/usr/bin/env bash
# Defines the require-changeset.sh contract.
#
# Sources require-changeset.sh so the unit tests can call its functions
# in-process; the Cli tests pipe into it instead, to cover stdin handling and
# exit codes.
#
# Run with: bash scripts/test-require-changeset.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REQUIRE_SH="$SCRIPT_DIR/require-changeset.sh"

# shellcheck source=./require-changeset.sh
source "$REQUIRE_SH"

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

# Asserts requires_changeset's verdict for one path.
assert_requires() {
  local path="$1"
  requires_changeset "$path"
  assert_status "$path requires a changeset" 0 "$?"
}

assert_exempt() {
  local path="$1"
  requires_changeset "$path"
  assert_status "$path is exempt" 1 "$?"
}

# Runs the script as the workflow does, with paths on stdin.
run_cli() {
  printf '%s\n' "$@" | bash "$REQUIRE_SH" 2>&1
}

run_cli_status() {
  printf '%s\n' "$@" | bash "$REQUIRE_SH" > /dev/null 2>&1
  printf '%s' "$?"
}

# -- which paths are release-relevant --

test_plugin_source_requires_a_changeset() {
  assert_requires "src/main/java/com/samleighton/sethomestwo/SetHomesTwo.java"
  assert_requires "src/main/resources/plugin.yml"
}

test_the_pom_requires_a_changeset() {
  assert_requires "pom.xml"
}

test_an_unrecognised_path_requires_a_changeset() {
  # Fail closed: a directory nobody has classified yet is treated as shippable
  # until someone deliberately exempts it.
  assert_requires "lang/en.yml"
  assert_requires "Dockerfile"
}

test_tests_are_exempt() {
  assert_exempt "src/test/java/com/samleighton/sethomestwo/updates/UpdateCheckerTest.java"
}

test_automation_is_exempt() {
  assert_exempt ".github/workflows/tests.yml"
  assert_exempt "scripts/release.sh"
}

test_docs_are_exempt() {
  assert_exempt "docs/img/logo.png"
  assert_exempt "README.md"
  assert_exempt "src/README.md"
  assert_exempt "CLAUDE.md"
}

test_repo_metadata_is_exempt() {
  assert_exempt "CODEOWNERS"
  assert_exempt ".gitignore"
  assert_exempt ".idea/misc.xml"
}

test_changesets_themselves_are_exempt() {
  assert_exempt ".changeset/brave-otters-sing.md"
}

# -- what counts as a changeset --

test_a_changeset_file_is_recognised() {
  is_changeset ".changeset/brave-otters-sing.md"
  assert_status "a .changeset md file is a changeset" 0 "$?"
}

test_a_readme_in_the_changeset_dir_is_not_a_changeset() {
  is_changeset ".changeset/README.md"
  assert_status ".changeset/README.md is not a changeset" 1 "$?"
}

test_a_markdown_file_elsewhere_is_not_a_changeset() {
  is_changeset "docs/notes.md"
  assert_status "a markdown file outside .changeset is not a changeset" 1 "$?"
}

# -- the command --

test_code_without_a_changeset_fails() {
  assert_status "code alone fails" 1 \
    "$(run_cli_status "src/main/java/Foo.java")"
}

test_code_with_a_changeset_passes() {
  assert_status "code plus a changeset passes" 0 \
    "$(run_cli_status "src/main/java/Foo.java" ".changeset/brave-otters-sing.md")"
}

test_docs_alone_pass() {
  assert_status "docs alone pass" 0 \
    "$(run_cli_status "README.md" "docs/img/logo.png")"
}

test_automation_alone_passes() {
  assert_status "automation alone passes" 0 \
    "$(run_cli_status ".github/workflows/tests.yml" "scripts/release.sh")"
}

test_docs_mixed_with_code_still_needs_a_changeset() {
  assert_status "docs plus code without a changeset fails" 1 \
    "$(run_cli_status "README.md" "src/main/java/Foo.java")"
}

test_an_empty_diff_passes() {
  assert_status "no changed files passes" 0 "$(printf '' | bash "$REQUIRE_SH" > /dev/null 2>&1; printf '%s' "$?")"
}

test_the_failure_names_the_offending_files() {
  local out
  out="$(run_cli "src/main/java/Foo.java" "README.md")"
  assert_contains "failure names the file that triggered it" "$out" "src/main/java/Foo.java"
  case "$out" in
    *README.md*) fail "failure does not list exempt files" "README.md should not appear" ;;
    *) pass "failure does not list exempt files" ;;
  esac
}

test_the_failure_explains_the_fix() {
  assert_contains "failure points at changeset.sh" \
    "$(run_cli "src/main/java/Foo.java")" "scripts/changeset.sh"
}

test_carriage_returns_are_tolerated() {
  assert_status "a CRLF path list is still classified" 1 \
    "$(printf 'src/main/java/Foo.java\r\n' | bash "$REQUIRE_SH" > /dev/null 2>&1; printf '%s' "$?")"
}

test_blank_lines_are_ignored() {
  assert_status "blank lines do not trigger the requirement" 0 \
    "$(printf 'README.md\n\n' | bash "$REQUIRE_SH" > /dev/null 2>&1; printf '%s' "$?")"
}

test_plugin_source_requires_a_changeset
test_the_pom_requires_a_changeset
test_an_unrecognised_path_requires_a_changeset
test_tests_are_exempt
test_automation_is_exempt
test_docs_are_exempt
test_repo_metadata_is_exempt
test_changesets_themselves_are_exempt

test_a_changeset_file_is_recognised
test_a_readme_in_the_changeset_dir_is_not_a_changeset
test_a_markdown_file_elsewhere_is_not_a_changeset

test_code_without_a_changeset_fails
test_code_with_a_changeset_passes
test_docs_alone_pass
test_automation_alone_passes
test_docs_mixed_with_code_still_needs_a_changeset
test_an_empty_diff_passes
test_the_failure_names_the_offending_files
test_the_failure_explains_the_fix
test_carriage_returns_are_tolerated
test_blank_lines_are_ignored

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0

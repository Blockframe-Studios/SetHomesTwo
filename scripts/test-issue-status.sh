#!/usr/bin/env bash
# Defines the issue-status.sh contract.
#
# Sources issue-status.sh so the tests can call its functions in-process.
#
# Run with: bash scripts/test-issue-status.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ISSUE_STATUS_SH="$SCRIPT_DIR/issue-status.sh"

# shellcheck source=./issue-status.sh
source "$ISSUE_STATUS_SH"

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

assert_equals() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    pass "$desc"
  else
    fail "$desc" "expected [$expected] got [$actual]"
  fi
}

# Runs closing_refs over a body and joins the result with commas, so an
# expectation reads as one string.
refs_of() {
  printf '%s' "$1" | closing_refs | paste -sd, -
}

# -- which references close an issue --

test_each_keyword_matches() {
  assert_equals "close"    "1" "$(refs_of 'close #1')"
  assert_equals "closes"   "2" "$(refs_of 'closes #2')"
  assert_equals "closed"   "3" "$(refs_of 'closed #3')"
  assert_equals "fix"      "4" "$(refs_of 'fix #4')"
  assert_equals "fixes"    "5" "$(refs_of 'fixes #5')"
  assert_equals "fixed"    "6" "$(refs_of 'fixed #6')"
  assert_equals "resolve"  "7" "$(refs_of 'resolve #7')"
  assert_equals "resolves" "8" "$(refs_of 'resolves #8')"
  assert_equals "resolved" "9" "$(refs_of 'resolved #9')"
}

test_keywords_are_case_insensitive() {
  assert_equals "Closes" "10" "$(refs_of 'Closes #10')"
  assert_equals "FIXES"  "11" "$(refs_of 'FIXES #11')"
}

test_a_colon_and_extra_space_are_tolerated() {
  assert_equals "colon form" "12" "$(refs_of 'Closes: #12')"
  assert_equals "wide space" "13" "$(refs_of 'Closes   #13')"
}

test_trailing_punctuation_is_not_part_of_the_number() {
  assert_equals "full stop" "53" "$(refs_of 'Closes #53.')"
  assert_equals "comma"     "53" "$(refs_of 'Closes #53, and more')"
}

test_a_bare_mention_does_not_close() {
  assert_equals "bare hash" "" "$(refs_of 'See #41 for background')"
}

test_a_keyword_inside_a_word_does_not_count() {
  assert_equals "supercloses" "" "$(refs_of 'supercloses #5')"
}

test_a_real_pull_request_body_yields_only_the_closed_issue() {
  local body
  body='Closes #53. First of the five sub-issues split out of #41.

The alternatives are recorded on #41 and were rejected.

- #46 (found during the in game check)
- The README change slightly overlaps #56.'
  assert_equals "PR 58 body closes only 53" "53" "$(refs_of "$body")"
}

test_several_references_are_all_returned() {
  assert_equals "two keywords" "1,2" "$(refs_of 'Closes #1 and fixes #2')"
}

test_a_repeated_reference_is_returned_once() {
  assert_equals "deduped" "7" "$(refs_of 'Closes #7. Also closes #7.')"
}

test_a_body_with_no_reference_is_empty_and_clean() {
  assert_equals "no refs" "" "$(refs_of 'Just a description.')"
  printf '%s' 'Just a description.' | closing_refs > /dev/null
  assert_equals "exit status" "0" "$?"
}

test_an_empty_body_is_empty_and_clean() {
  assert_equals "empty" "" "$(refs_of '')"
  printf '%s' '' | closing_refs > /dev/null
  assert_equals "exit status" "0" "$?"
}

test_issue_zero_is_rejected() {
  assert_equals "hash zero" "" "$(refs_of 'Closes #0')"
}

test_a_non_numeric_reference_is_rejected() {
  assert_equals "hash word" "" "$(refs_of 'Closes #abc')"
}

# Runs pr_numbers_from_log over log subjects and joins the result with commas.
prs_of() {
  printf '%s' "$1" | pr_numbers_from_log | paste -sd, -
}

# -- which pull requests reached a commit range --

test_a_merge_subject_yields_its_number() {
  assert_equals "merge commit" "58" \
    "$(prs_of 'Merge pull request #58 from Blockframe-Studios/issue-53-refuse-alongside-v1')"
}

test_a_squash_subject_yields_its_number() {
  assert_equals "squash commit" "44" \
    "$(prs_of 'Tab completion matches anywhere in a name (#44)')"
}

test_a_release_commit_yields_nothing() {
  assert_equals "release commit" "" "$(prs_of 'chore(release): 1.2.3')"
}

test_an_ordinary_commit_yields_nothing() {
  assert_equals "plain commit" "" "$(prs_of 'fix: send the BukkitDev metadata with --form-string')"
}

# A subject may cite an issue without the commit having come from that pull
# request. Only the merge and squash forms count.
test_a_mention_in_a_subject_is_not_a_pull_request() {
  assert_equals "bare mention" "" "$(prs_of 'fix: address feedback on #41')"
}

test_an_empty_range_is_empty_and_clean() {
  assert_equals "empty range" "" "$(prs_of '')"
  printf '%s' '' | pr_numbers_from_log > /dev/null
  assert_equals "exit status" "0" "$?"
}

test_repeated_numbers_collapse() {
  local log
  log='Merge pull request #31 from Blockframe-Studios/fix/bukkitdev-metadata-semicolon
Merge pull request #31 from Blockframe-Studios/fix/bukkitdev-metadata-semicolon'
  assert_equals "deduped" "31" "$(prs_of "$log")"
}

test_several_merges_keep_first_appearance_order() {
  local log
  log='Merge pull request #58 from Blockframe-Studios/issue-53-refuse-alongside-v1
Merge pull request #52 from Blockframe-Studios/issue-49-import-report-colour-codes'
  assert_equals "order kept" "58,52" "$(prs_of "$log")"
}

test_each_keyword_matches
test_keywords_are_case_insensitive
test_a_colon_and_extra_space_are_tolerated
test_trailing_punctuation_is_not_part_of_the_number
test_a_bare_mention_does_not_close
test_a_keyword_inside_a_word_does_not_count
test_a_real_pull_request_body_yields_only_the_closed_issue
test_several_references_are_all_returned
test_a_repeated_reference_is_returned_once
test_a_body_with_no_reference_is_empty_and_clean
test_an_empty_body_is_empty_and_clean
test_issue_zero_is_rejected
test_a_non_numeric_reference_is_rejected
test_a_merge_subject_yields_its_number
test_a_squash_subject_yields_its_number
test_a_release_commit_yields_nothing
test_an_ordinary_commit_yields_nothing
test_a_mention_in_a_subject_is_not_a_pull_request
test_an_empty_range_is_empty_and_clean
test_repeated_numbers_collapse
test_several_merges_keep_first_appearance_order

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0

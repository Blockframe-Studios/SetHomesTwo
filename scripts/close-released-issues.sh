#!/usr/bin/env bash
# Closes the issues contained in the commit being released.
#
# Reads PREV_TAG (may be empty on a first release) and VERSION from the
# environment. With DRY_RUN=1 it prints what it would close and closes nothing.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./issue-status.sh
source "$SCRIPT_DIR/issue-status.sh"

VERSION="${VERSION:?VERSION must be set}"
PREV_TAG="${PREV_TAG:-}"
DRY_RUN="${DRY_RUN:-0}"

range="HEAD"
if [ -n "$PREV_TAG" ]; then
  range="$PREV_TAG..HEAD"
fi
printf 'Range: %s\n' "$range"

# Subjects only. A commit body may mention a pull request it did not come from.
prs="$(git log --format=%s "$range" | pr_numbers_from_log)"
if [ -z "$prs" ]; then
  echo "No pull requests in the range - nothing to close."
  exit 0
fi

issues=""
while IFS= read -r pr; do
  [ -n "$pr" ] || continue
  body="$(gh pr view "$pr" --json body --jq '.body')" || continue
  refs="$(printf '%s' "$body" | closing_refs)"
  [ -n "$refs" ] || continue
  issues="$(printf '%s\n%s' "$issues" "$refs")"
done <<< "$prs"

issues="$(printf '%s' "$issues" | grep -E '^[1-9][0-9]*$' | awk '!seen[$0]++')"
if [ -z "$issues" ]; then
  echo "No closing references among those pull requests - nothing to close."
  exit 0
fi

while IFS= read -r issue; do
  [ -n "$issue" ] || continue

  state="$(gh issue view "$issue" --json state --jq '.state')" || continue
  if [ "$state" != "OPEN" ]; then
    printf '#%s is already %s - skipping.\n' "$issue" "$state"
    continue
  fi

  if [ "$DRY_RUN" = "1" ]; then
    printf 'Would close #%s (Released in v%s)\n' "$issue" "$VERSION"
    continue
  fi

  gh issue close "$issue" --reason completed --comment "Released in v$VERSION"
  printf 'Closed #%s\n' "$issue"
done <<< "$issues"

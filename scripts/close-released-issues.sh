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
pr_count=0
pr_failures=0
while IFS= read -r pr; do
  [ -n "$pr" ] || continue
  pr_count=$((pr_count + 1))
  if ! body="$(gh pr view "$pr" --json body --jq '.body')"; then
    printf 'Warning: could not look up pull request #%s - skipping it.\n' "$pr" >&2
    pr_failures=$((pr_failures + 1))
    continue
  fi
  refs="$(printf '%s' "$body" | closing_refs)"
  [ -n "$refs" ] || continue
  issues="$(printf '%s\n%s' "$issues" "$refs")"
done <<< "$prs"

if [ "$pr_count" -gt 0 ] && [ "$pr_failures" -eq "$pr_count" ]; then
  printf 'Error: all %d pull request lookups failed - cannot tell what this release closes.\n' "$pr_count" >&2
  exit 1
fi

issues="$(printf '%s' "$issues" | grep -E '^[1-9][0-9]*$' | awk '!seen[$0]++')"
if [ -z "$issues" ]; then
  echo "No closing references among those pull requests - nothing to close."
  exit 0
fi

close_failures=0
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

  if gh issue close "$issue" --reason completed --comment "Released in v$VERSION"; then
    printf 'Closed #%s\n' "$issue"
  else
    printf 'Failed to close #%s - close it by hand.\n' "$issue" >&2
    close_failures=$((close_failures + 1))
  fi
done <<< "$issues"

if [ "$close_failures" -gt 0 ]; then
  exit 1
fi

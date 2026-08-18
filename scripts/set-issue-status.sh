#!/usr/bin/env bash
# Sets an issue's Status on the SetHomesTwo project board.
#
#   bash scripts/set-issue-status.sh 54 "In review"
#   bash scripts/set-issue-status.sh 54 "In progress" Todo unset
#
# The optional trailing arguments guard the write: the status is set only when
# the item currently holds one of them, where `unset` means no status yet. That
# is what stops a later push to an issue branch pulling the issue back out of
# In review.
#
# Needs GH_TOKEN with the project scope; GITHUB_TOKEN cannot reach an
# organization project.
set -uo pipefail

ORG="Blockframe-Studios"
REPO="SetHomesTwo"
PROJECT_NUMBER="${PROJECT_NUMBER:?PROJECT_NUMBER must be set}"

ISSUE="${1:?issue number required}"
TARGET_STATUS="${2:?target status required}"
shift 2
ALLOWED_CURRENT=("$@")

PROJECT_QUERY='
  query($org:String!, $number:Int!) {
    organization(login:$org) {
      projectV2(number:$number) {
        id
        field(name:"Status") {
          ... on ProjectV2SingleSelectField { id options { id name } }
        }
      }
    }
  }'

# gh has jq built in. Standalone jq is not installed on the development machine,
# and every line here has to run locally as well as on a runner.
ids="$(gh api graphql -f query="$PROJECT_QUERY" -f org="$ORG" \
  -F number="$PROJECT_NUMBER" \
  --jq '[.data.organization.projectV2.id,
         .data.organization.projectV2.field.id] | @tsv')" || exit 1
IFS=$'\t' read -r project_id field_id <<<"$ids"

options="$(gh api graphql -f query="$PROJECT_QUERY" -f org="$ORG" \
  -F number="$PROJECT_NUMBER" \
  --jq '.data.organization.projectV2.field.options[] | [.name, .id] | @tsv')" || exit 1

# Matched in awk rather than inside the jq program, so the status name is never
# interpolated into a query.
option_id="$(printf '%s\n' "$options" \
  | awk -F'\t' -v n="$TARGET_STATUS" '$1 == n { print $2; exit }')"

if [ -z "$option_id" ]; then
  printf 'No Status option named [%s] on the project.\n' "$TARGET_STATUS" >&2
  exit 1
fi

ISSUE_QUERY='
  query($owner:String!, $repo:String!, $issue:Int!) {
    repository(owner:$owner, name:$repo) {
      issue(number:$issue) {
        id
        projectItems(first:20) {
          nodes {
            id
            project { id }
            fieldValueByName(name:"Status") {
              ... on ProjectV2ItemFieldSingleSelectValue { name }
            }
          }
        }
      }
    }
  }'

issue_id="$(gh api graphql -f query="$ISSUE_QUERY" -f owner="$ORG" -f repo="$REPO" \
  -F issue="$ISSUE" --jq '.data.repository.issue.id')" || exit 1
if [ -z "$issue_id" ]; then
  printf 'Issue #%s not found - nothing to do.\n' "$ISSUE"
  exit 0
fi

items="$(gh api graphql -f query="$ISSUE_QUERY" -f owner="$ORG" -f repo="$REPO" \
  -F issue="$ISSUE" \
  --jq '.data.repository.issue.projectItems.nodes[]
          | [.project.id, .id, (.fieldValueByName.name // "unset")] | @tsv')" || exit 1

item_id="$(printf '%s\n' "$items" \
  | awk -F'\t' -v p="$project_id" '$1 == p { print $2; exit }')"
current_status="$(printf '%s\n' "$items" \
  | awk -F'\t' -v p="$project_id" '$1 == p { print $3; exit }')"

if [ -z "$item_id" ] || [ "$item_id" = "null" ]; then
  current_status="unset"
  # Idempotent: returns the existing item when the issue is already on the board.
  item_id="$(gh api graphql -f query='
    mutation($project:ID!, $content:ID!) {
      addProjectV2ItemById(input:{projectId:$project, contentId:$content}) {
        item { id }
      }
    }' -f project="$project_id" -f content="$issue_id" \
    --jq '.data.addProjectV2ItemById.item.id')" || exit 1
fi

if [ "${#ALLOWED_CURRENT[@]}" -gt 0 ]; then
  allowed=1
  for candidate in "${ALLOWED_CURRENT[@]}"; do
    if [ "$candidate" = "$current_status" ]; then
      allowed=0
      break
    fi
  done
  if [ "$allowed" -ne 0 ]; then
    printf '#%s is [%s], not one of [%s] - leaving it alone.\n' \
      "$ISSUE" "$current_status" "${ALLOWED_CURRENT[*]}"
    exit 0
  fi
fi

gh api graphql -f query='
  mutation($project:ID!, $item:ID!, $field:ID!, $option:String!) {
    updateProjectV2ItemFieldValue(input:{
      projectId:$project, itemId:$item, fieldId:$field,
      value:{ singleSelectOptionId:$option }
    }) { projectV2Item { id } }
  }' -f project="$project_id" -f item="$item_id" -f field="$field_id" \
     -f option="$option_id" > /dev/null || exit 1

printf '#%s: %s -> %s\n' "$ISSUE" "$current_status" "$TARGET_STATUS"

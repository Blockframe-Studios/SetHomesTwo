#!/usr/bin/env bash
# Write a changeset file describing how this change affects the version.
#
# Pure bash - no jq, no python. This is the command contributors run, and
# staying free of extra tooling is the entire point.
#
# Run with no arguments for prompts, or:
#   bash scripts/changeset.sh minor "Summary of the change"

BUMPS=(patch minor major)

DESCRIPTIONS_patch="bug fix, no behaviour change for existing setups"
DESCRIPTIONS_minor="new functionality, existing setups keep working"
DESCRIPTIONS_major=$'breaking - a command, permission, config key or\n               runtime requirement changes for existing servers'

ADJECTIVES=(brave calm clever eager gentle happy kind lucky proud quiet swift tidy warm wise)
ANIMALS=(badgers cranes dolphins foxes herons lynxes otters pandas ravens seals tigers wolves)
VERBS=(arrive dance gather listen return shine sing smile travel wander wave wonder)

_description_for() {
  case "$1" in
    patch) printf '%s' "$DESCRIPTIONS_patch" ;;
    minor) printf '%s' "$DESCRIPTIONS_minor" ;;
    major) printf '%s' "$DESCRIPTIONS_major" ;;
  esac
}

_is_valid_bump() {
  case "$1" in
    patch | minor | major) return 0 ;;
    *) return 1 ;;
  esac
}

_trim() {
  local s="$1"
  while [[ "$s" == [$' \t']* ]]; do s="${s# }"; s="${s#	}"; done
  while [[ "$s" == *[$' \t'] ]]; do s="${s% }"; s="${s%	}"; done
  printf '%s' "$s"
}

random_name() {
  local a="${ADJECTIVES[$((RANDOM % ${#ADJECTIVES[@]}))]}"
  local b="${ANIMALS[$((RANDOM % ${#ANIMALS[@]}))]}"
  local c="${VERBS[$((RANDOM % ${#VERBS[@]}))]}"
  printf '%s-%s-%s' "$a" "$b" "$c"
}

# Sets PROMPTED_BUMP.
prompt_bump() {
  printf '\n  What kind of change is this?\n\n'
  local i=1 b
  for b in "${BUMPS[@]}"; do
    printf '    %d) %-7s %s\n' "$i" "$b" "$(_description_for "$b")"
    i=$((i + 1))
  done
  printf '\n'

  local choice
  while true; do
    if ! read -r -p "  > " choice; then
      # stdin closed (e.g. non-interactive) - nothing sensible to prompt with.
      printf '  No input available.\n' >&2
      return 1
    fi
    case "$choice" in
      1) PROMPTED_BUMP="patch"; return 0 ;;
      2) PROMPTED_BUMP="minor"; return 0 ;;
      3) PROMPTED_BUMP="major"; return 0 ;;
      patch | minor | major) PROMPTED_BUMP="$choice"; return 0 ;;
      *) printf '  Enter 1, 2 or 3.\n' ;;
    esac
  done
}

# Sets PROMPTED_SUMMARY.
prompt_summary() {
  printf '\n  Summary (one line, written for server owners):\n'
  local summary
  while true; do
    if ! read -r -p "  > " summary; then
      printf '  No input available.\n' >&2
      return 1
    fi
    summary="$(_trim "$summary")"
    if [ -n "$summary" ]; then
      PROMPTED_SUMMARY="$summary"
      return 0
    fi
    printf '  A summary is required.\n'
  done
}

# Sets WRITE_PATH.
write_changeset() {
  local bump="$1" summary="$2" changeset_dir="$3"
  mkdir -p "$changeset_dir"

  local name path
  name="$(random_name)"
  path="${changeset_dir}/${name}.md"
  while [ -e "$path" ]; do
    name="$(random_name)"
    path="${changeset_dir}/${name}.md"
  done

  printf -- '---\nbump: %s\n---\n\n%s\n' "$bump" "$summary" > "$path"
  WRITE_PATH="$path"
}

main() {
  local script_dir repo_root changeset_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  repo_root="$(cd "$script_dir/.." && pwd)"
  changeset_dir="$repo_root/.changeset"

  local bump="" summary=""

  if [ $# -ge 2 ]; then
    bump="$1"
    shift
    summary="$*"
    if ! _is_valid_bump "$bump"; then
      echo "bump must be one of: patch, minor, major" >&2
      return 1
    fi
  elif [ $# -eq 1 ]; then
    echo 'usage: changeset.sh [patch|minor|major "summary"]' >&2
    return 1
  else
    if ! prompt_bump; then
      return 1
    fi
    bump="$PROMPTED_BUMP"
    if ! prompt_summary; then
      return 1
    fi
    summary="$PROMPTED_SUMMARY"
  fi

  write_changeset "$bump" "$summary" "$changeset_dir"
  local path="$WRITE_PATH"

  local relative
  if command -v realpath > /dev/null 2>&1; then
    relative="$(realpath --relative-to="$(pwd)" "$path" 2> /dev/null)"
  fi
  if [ -z "${relative:-}" ]; then
    relative="$path"
  fi

  local staged_note=""
  if command -v git > /dev/null 2>&1; then
    if git add "$path" > /dev/null 2>&1; then
      staged_note="  (staged)"
    fi
  fi

  printf '\n  Created %s%s\n\n' "$relative" "$staged_note"
  return 0
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set -uo pipefail
  main "$@"
  exit $?
fi

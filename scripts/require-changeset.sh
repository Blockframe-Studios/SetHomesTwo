#!/usr/bin/env bash
# Fails a pull request that changes something shippable without adding a changeset.
#
# Reads the changed paths on stdin, one per line:
#   gh api .../pulls/N/files --jq '.[].filename' | bash scripts/require-changeset.sh
#
# Exit 0 - nothing shippable changed, or a changeset is present.
# Exit 1 - shippable files changed and no changeset came with them.

CHANGESET_DIR=".changeset"

# Nothing under these prefixes reaches a user's server, so changing them
# releases nothing.
EXEMPT_PREFIXES=(
  "$CHANGESET_DIR/"
  ".github/"
  ".idea/"
  "docs/"
  "scripts/"
  "src/test/"
)

EXEMPT_FILES=(
  "CODEOWNERS"
  ".gitattributes"
  ".gitconfig"
  ".gitignore"
)

# Returns 0 when changing this path should force a version bump. Anything not
# explicitly exempted counts: a directory nobody has classified yet is more
# safely treated as shippable than waved through.
requires_changeset() {
  local path="$1" prefix file

  # Markdown is documentation wherever it lives; none of it ships in the jar.
  case "$path" in
    *.md) return 1 ;;
  esac

  for prefix in "${EXEMPT_PREFIXES[@]}"; do
    case "$path" in
      "$prefix"*) return 1 ;;
    esac
  done

  for file in "${EXEMPT_FILES[@]}"; do
    if [ "$path" = "$file" ]; then
      return 1
    fi
  done

  return 0
}

# Returns 0 for a changeset written by changeset.sh. The directory's own README
# is documentation, and must not be able to satisfy the requirement.
is_changeset() {
  local path="$1"
  case "$path" in
    "$CHANGESET_DIR"/README.md) return 1 ;;
    "$CHANGESET_DIR"/*.md) return 0 ;;
    *) return 1 ;;
  esac
}

main() {
  local line path has_changeset=1
  local shippable=()

  while IFS= read -r line || [ -n "$line" ]; do
    # gh writes LF, but a hand-piped list on Windows may not.
    path="${line%$'\r'}"
    [ -n "$path" ] || continue

    if is_changeset "$path"; then
      has_changeset=0
    fi

    if requires_changeset "$path"; then
      shippable+=("$path")
    fi
  done

  if [ "${#shippable[@]}" -eq 0 ]; then
    printf 'No shippable files changed - no changeset needed.\n'
    return 0
  fi

  if [ "$has_changeset" -eq 0 ]; then
    printf 'Changeset present for %d shippable file(s).\n' "${#shippable[@]}"
    return 0
  fi

  {
    printf 'This pull request changes files that ship to users but adds no changeset:\n'
    printf '  %s\n' "${shippable[@]}"
    printf '\nAdd one with:  bash scripts/changeset.sh\n'
  } >&2
  return 1
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set -uo pipefail
  main "$@"
  exit $?
fi

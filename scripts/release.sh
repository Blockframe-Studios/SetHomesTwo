#!/usr/bin/env bash
# Compute and apply a release from the changeset files in .changeset/.
#
# Split into `plan` (compute, touch nothing), `entries` (list summaries,
# touch nothing), `notes` (read the changelog, touch nothing) and `apply`
# (edit the README, delete consumed changesets) so the release workflow's
# dry-run can call `plan` and be guaranteed to write nothing.
#
# Every function below communicates through global variables rather than by
# printing to stdout and being captured with `$(...)`, on purpose: command
# substitution strips trailing newlines, which would silently corrupt the
# changelog text this script edits. Only plain CLI output (the key=value
# lines, `entries`, `notes`) goes to stdout.
#
# Usage:
#   release.sh plan   --current-version X --changeset-dir D
#   release.sh entries --changeset-dir D
#   release.sh apply  --current-version X --changeset-dir D --readme README.md --date YYYY-MM-DD
#   release.sh notes  --readme README.md --version X

CHANGELOG_HEADING=$'### Changelog\n'

# ---------------------------------------------------------------------------
# parse_changeset TEXT
#
# Frontmatter is '---', 'bump: <patch|minor|major>', '---', then the
# summary. Tolerates trailing whitespace and CRLF line endings. A multi-line
# summary joins into one line, single-spaced.
#
# On success sets PARSED_BUMP, PARSED_SUMMARY and returns 0.
# On failure sets PARSE_ERROR and returns 1.
# ---------------------------------------------------------------------------
parse_changeset() {
  local text="$1"
  text="${text//$'\r'/}"

  local -a lines
  mapfile -t lines <<< "$text"

  if [ "${#lines[@]}" -lt 3 ]; then
    PARSE_ERROR="changeset must start with '---', a 'bump:' line, then '---'"
    return 1
  fi

  local l1 l2 l3
  l1="$(_trim "${lines[0]}")"
  l2="$(_trim "${lines[1]}")"
  l3="$(_trim "${lines[2]}")"

  if [ "$l1" != "---" ] || [ "$l3" != "---" ]; then
    PARSE_ERROR="changeset must start with '---', a 'bump:' line, then '---'"
    return 1
  fi

  local bump
  if [[ "$l2" =~ ^bump:[[:space:]]*([^[:space:]]+)$ ]]; then
    bump="${BASH_REMATCH[1]}"
  else
    PARSE_ERROR="changeset must start with '---', a 'bump:' line, then '---'"
    return 1
  fi

  if ! _is_valid_bump "$bump"; then
    PARSE_ERROR="bump must be one of patch, minor, major, got '${bump}'"
    return 1
  fi

  local summary="" i t
  for ((i = 3; i < ${#lines[@]}; i++)); do
    t="$(_trim "${lines[$i]}")"
    if [ -n "$t" ]; then
      if [ -n "$summary" ]; then
        summary="$summary $t"
      else
        summary="$t"
      fi
    fi
  done

  if [ -z "$summary" ]; then
    PARSE_ERROR="changeset has no summary text"
    return 1
  fi

  PARSED_BUMP="$bump"
  PARSED_SUMMARY="$summary"
  return 0
}

_is_valid_bump() {
  case "$1" in
    patch | minor | major) return 0 ;;
    *) return 1 ;;
  esac
}

_trim() {
  local s="$1"
  s="${s%$'\r'}"
  # Strip leading/trailing space and tab (whitespace \s in the original
  # Python regex, minus the newlines mapfile already split on).
  while [[ "$s" == [$' \t']* ]]; do s="${s# }"; s="${s#	}"; done
  while [[ "$s" == *[$' \t'] ]]; do s="${s% }"; s="${s%	}"; done
  printf '%s' "$s"
}

# ---------------------------------------------------------------------------
# highest_bump BUMP...
#
# Sets HIGHEST_BUMP to whichever of the given bumps ranks highest
# (major > minor > patch) and returns 0, or sets HIGHEST_BUMP_ERROR and
# returns 1 if any argument is not a recognised bump.
# ---------------------------------------------------------------------------
highest_bump() {
  local best="" best_rank=-1 b r
  for b in "$@"; do
    case "$b" in
      patch) r=0 ;;
      minor) r=1 ;;
      major) r=2 ;;
      *)
        HIGHEST_BUMP_ERROR="unknown bump '${b}'"
        return 1
        ;;
    esac
    if [ "$r" -gt "$best_rank" ]; then
      best_rank="$r"
      best="$b"
    fi
  done
  HIGHEST_BUMP="$best"
  return 0
}

# ---------------------------------------------------------------------------
# next_version CURRENT BUMP
#
# CURRENT must be exactly MAJOR.MINOR.PATCH of digits - anything else
# (e.g. "1.2.0-SNAPSHOT") is rejected before any arithmetic runs.
#
# Sets NEXT_VERSION and returns 0, or sets NEXT_VERSION_ERROR and returns 1.
# ---------------------------------------------------------------------------
next_version() {
  local current="$1" bump="$2"

  if [[ ! "$current" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    NEXT_VERSION_ERROR="version must be MAJOR.MINOR.PATCH of digits, got '${current}'"
    return 1
  fi

  local major minor patch
  major=$((10#${BASH_REMATCH[1]}))
  minor=$((10#${BASH_REMATCH[2]}))
  patch=$((10#${BASH_REMATCH[3]}))

  case "$bump" in
    major) NEXT_VERSION="$((major + 1)).0.0" ;;
    minor) NEXT_VERSION="${major}.$((minor + 1)).0" ;;
    patch) NEXT_VERSION="${major}.${minor}.$((patch + 1))" ;;
    *)
      NEXT_VERSION_ERROR="unknown bump '${bump}'"
      return 1
      ;;
  esac
  return 0
}

# ---------------------------------------------------------------------------
# render_section VERSION DATE ENTRY...
#
# Sets RENDERED_SECTION to "#### VERSION (DATE)\n\n- entry\n..." (always
# ending in exactly one newline). Always succeeds.
# ---------------------------------------------------------------------------
render_section() {
  local version="$1" date="$2"
  shift 2
  local out="#### ${version} (${date})"$'\n\n'
  local e
  for e in "$@"; do
    out+="- ${e}"$'\n'
  done
  RENDERED_SECTION="$out"
  return 0
}

# ---------------------------------------------------------------------------
# insert_changelog README_TEXT SECTION
#
# Inserts SECTION directly under the "### Changelog" heading in README_TEXT.
#
# Sets INSERTED_TEXT and returns 0, or sets INSERT_ERROR and returns 1 if
# README_TEXT has no such heading.
# ---------------------------------------------------------------------------
insert_changelog() {
  local readme="$1" section="$2"

  if [[ "$readme" != *"$CHANGELOG_HEADING"* ]]; then
    INSERT_ERROR="README has no '### Changelog' heading"
    return 1
  fi

  local before after
  before="${readme%%"$CHANGELOG_HEADING"*}"
  after="${readme#*"$CHANGELOG_HEADING"}"

  while [[ "$after" == $'\n'* ]]; do
    after="${after#$'\n'}"
  done

  INSERTED_TEXT="${before}${CHANGELOG_HEADING}"$'\n'"${section}"$'\n'"${after}"
  return 0
}

# ---------------------------------------------------------------------------
# collect DIR
#
# Reads every *.md changeset in DIR (sorted, skipping README.md
# case-insensitively). Sets COLLECT_PATHS, COLLECT_BUMPS, COLLECT_ENTRIES
# (parallel arrays) and returns 0, or sets COLLECT_ERROR and returns 1 on
# the first malformed file.
# ---------------------------------------------------------------------------
collect() {
  local dir="$1"
  COLLECT_PATHS=()
  COLLECT_BUMPS=()
  COLLECT_ENTRIES=()
  COLLECT_ERROR=""

  local -a files
  mapfile -t files < <(printf '%s\n' "$dir"/*.md 2> /dev/null | sort)

  local f base lc content
  for f in "${files[@]}"; do
    [ -e "$f" ] || continue
    base="$(basename "$f")"
    lc="$(printf '%s' "$base" | tr '[:upper:]' '[:lower:]')"
    if [ "$lc" = "readme.md" ]; then
      continue
    fi

    content="$(cat "$f")"
    if ! parse_changeset "$content"; then
      COLLECT_ERROR="${f}: ${PARSE_ERROR}"
      return 1
    fi

    COLLECT_PATHS+=("$f")
    COLLECT_BUMPS+=("$PARSED_BUMP")
    COLLECT_ENTRIES+=("$PARSED_SUMMARY")
  done
  return 0
}

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

_usage() {
  cat >&2 << 'EOF'
usage:
  release.sh plan    --current-version X --changeset-dir D
  release.sh entries --changeset-dir D
  release.sh apply   --current-version X --changeset-dir D --readme README.md --date YYYY-MM-DD
  release.sh notes   --readme README.md --version X
EOF
}

cmd_plan() {
  local current="" dir=".changeset"
  while [ $# -gt 0 ]; do
    case "$1" in
      --current-version)
        current="$2"
        shift 2
        ;;
      --changeset-dir)
        dir="$2"
        shift 2
        ;;
      *)
        echo "unknown argument: $1" >&2
        return 1
        ;;
    esac
  done
  if [ -z "$current" ]; then
    echo "--current-version is required" >&2
    return 1
  fi

  if ! collect "$dir"; then
    echo "$COLLECT_ERROR" >&2
    return 1
  fi
  if [ "${#COLLECT_PATHS[@]}" -eq 0 ]; then
    echo "no changesets in $dir - nothing to release" >&2
    return 3
  fi

  if ! highest_bump "${COLLECT_BUMPS[@]}"; then
    echo "$HIGHEST_BUMP_ERROR" >&2
    return 1
  fi
  local bump="$HIGHEST_BUMP"

  if ! next_version "$current" "$bump"; then
    echo "$NEXT_VERSION_ERROR" >&2
    return 1
  fi

  printf 'bump=%s\n' "$bump"
  printf 'current=%s\n' "$current"
  printf 'next=%s\n' "$NEXT_VERSION"
  return 0
}

cmd_entries() {
  local dir=".changeset"
  while [ $# -gt 0 ]; do
    case "$1" in
      --changeset-dir)
        dir="$2"
        shift 2
        ;;
      *)
        echo "unknown argument: $1" >&2
        return 1
        ;;
    esac
  done

  if ! collect "$dir"; then
    echo "$COLLECT_ERROR" >&2
    return 1
  fi

  local e
  for e in "${COLLECT_ENTRIES[@]}"; do
    printf -- '- %s\n' "$e"
  done
  return 0
}

cmd_apply() {
  local current="" dir=".changeset" readme="README.md" date=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --current-version)
        current="$2"
        shift 2
        ;;
      --changeset-dir)
        dir="$2"
        shift 2
        ;;
      --readme)
        readme="$2"
        shift 2
        ;;
      --date)
        date="$2"
        shift 2
        ;;
      *)
        echo "unknown argument: $1" >&2
        return 1
        ;;
    esac
  done
  if [ -z "$current" ]; then
    echo "--current-version is required" >&2
    return 1
  fi

  if ! collect "$dir"; then
    echo "$COLLECT_ERROR" >&2
    return 1
  fi
  if [ "${#COLLECT_PATHS[@]}" -eq 0 ]; then
    echo "no changesets in $dir - nothing to release" >&2
    return 3
  fi

  if ! highest_bump "${COLLECT_BUMPS[@]}"; then
    echo "$HIGHEST_BUMP_ERROR" >&2
    return 1
  fi
  local bump="$HIGHEST_BUMP"

  if ! next_version "$current" "$bump"; then
    echo "$NEXT_VERSION_ERROR" >&2
    return 1
  fi
  local next="$NEXT_VERSION"

  if [ -z "$date" ]; then
    echo "apply requires --date" >&2
    return 1
  fi

  if [ ! -f "$readme" ]; then
    echo "no such file: $readme" >&2
    return 1
  fi

  local readme_text=""
  IFS= read -r -d '' readme_text < "$readme" || true

  render_section "$next" "$date" "${COLLECT_ENTRIES[@]}"
  local section="$RENDERED_SECTION"

  if ! insert_changelog "$readme_text" "$section"; then
    echo "$INSERT_ERROR" >&2
    return 1
  fi

  printf '%s' "$INSERTED_TEXT" > "$readme"

  local p
  for p in "${COLLECT_PATHS[@]}"; do
    rm -f "$p"
  done

  printf 'bump=%s\n' "$bump"
  printf 'current=%s\n' "$current"
  printf 'next=%s\n' "$next"
  return 0
}

cmd_notes() {
  local readme="README.md" version=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --readme)
        readme="$2"
        shift 2
        ;;
      --version)
        version="$2"
        shift 2
        ;;
      *)
        echo "unknown argument: $1" >&2
        return 1
        ;;
    esac
  done
  if [ -z "$version" ]; then
    echo "--version is required" >&2
    return 1
  fi
  if [ ! -f "$readme" ]; then
    echo "no such file: $readme" >&2
    return 1
  fi

  awk -v version="$version" '
    BEGIN { heading = "#### " version " ("; capturing = 0; nbuf = 0 }
    {
      line = $0
      sub(/\r$/, "", line)
      if (!capturing) {
        if (index(line, heading) == 1) { capturing = 1 }
        next
      }
      if (index(line, "#### ") == 1) { exit }
      buf[++nbuf] = line
    }
    END {
      start = 1
      while (start <= nbuf && buf[start] ~ /^[ \t]*$/) start++
      last = nbuf
      while (last >= start && buf[last] ~ /^[ \t]*$/) last--
      for (i = start; i <= last; i++) print buf[i]
    }
  ' "$readme"
  return 0
}

main() {
  local cmd="${1:-}"
  if [ $# -ge 1 ]; then shift; fi
  case "$cmd" in
    plan) cmd_plan "$@" ;;
    entries) cmd_entries "$@" ;;
    apply) cmd_apply "$@" ;;
    notes) cmd_notes "$@" ;;
    *)
      _usage
      return 1
      ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set -euo pipefail
  main "$@"
  exit $?
fi

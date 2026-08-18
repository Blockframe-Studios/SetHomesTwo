#!/usr/bin/env bash
# Defines the BukkitDev upload contract. Stubs curl and asserts what the
# script hands it, chiefly that the metadata JSON arrives whole.
#
# Needs jq, as the script does. Run with:
#   bash scripts/test-publish-bukkitdev.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBLISH_SH="$SCRIPT_DIR/publish-bukkitdev.sh"

if ! command -v jq >/dev/null 2>&1; then
  # Never skip on CI: a missing jq there means the release itself would break.
  if [ -n "${CI:-}" ]; then
    echo "jq is not installed on CI" >&2
    exit 1
  fi
  echo "SKIP - jq is not installed, so publish-bukkitdev.sh cannot run" >&2
  exit 0
fi

PASS=0
FAIL=0

pass() {
  PASS=$((PASS + 1))
  printf 'ok   - %s\n' "$1"
}

fail() {
  FAIL=$((FAIL + 1))
  printf 'FAIL - %s\n' "$1"
  printf '       %s\n' "$2"
}

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Characters the transport or the JSON encoding could eat.
SEMICOLON_TEXT='held back for a week; a newer release is still announced'
QUOTE_TEXT='the "display" name and a C:\path'

mkdir -p "$WORK/run"
cat > "$WORK/run/README.md" <<README
# SetHomesTwo

## Changelog

#### 9.9.9 (2026-08-15)

- Fixed the notice repeating on every join. It is $SEMICOLON_TEXT straight away.
- Handles $QUOTE_TEXT correctly.

#### 9.9.8 (2026-08-14)

- An earlier release.
README

# One file per argument. jq pretty-prints, so the metadata argument spans
# several lines and any line-based log would split it.
CURL_ARGS_DIR="$WORK/args"
export CURL_ARGS_DIR

mkdir -p "$WORK/bin"
cat > "$WORK/bin/curl" <<'STUB'
#!/usr/bin/env bash
for arg in "$@"; do
  case "$arg" in
    *game/versions*)
      echo '[{"id":16500,"gameVersionTypeID":1,"name":"1.21.4"},
             {"id":11515,"gameVersionTypeID":1,"name":"1.21"},
             {"id":99999,"gameVersionTypeID":3,"name":"26.0.0"}]'
      exit 0
      ;;
  esac
done
mkdir -p "$CURL_ARGS_DIR"
i=0
for arg in "$@"; do
  printf '%s' "$arg" > "$CURL_ARGS_DIR/$i"
  i=$((i + 1))
done
echo '{"id":4242}'
echo 200
STUB
chmod +x "$WORK/bin/curl"

OUTPUT=$(cd "$WORK/run" && PATH="$WORK/bin:$PATH" CURSEFORGE_TOKEN=test-token VERSION=9.9.9 \
  bash "$PUBLISH_SH" 2>&1)
STATUS=$?

if [ "$STATUS" -eq 0 ]; then
  pass "publishes successfully against a stubbed API"
else
  fail "publishes successfully against a stubbed API" "exit $STATUS: $OUTPUT"
fi

if [ ! -d "$CURL_ARGS_DIR" ]; then
  fail "calls curl to upload" "no upload call recorded"
  printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
  exit 1
fi

# Whether curl was passed this exact argument.
has_arg() {
  local want=$1 file
  for file in "$CURL_ARGS_DIR"/*; do
    [ -f "$file" ] || continue
    if [ "$(cat "$file")" = "$want" ]; then
      return 0
    fi
  done
  return 1
}

args_summary() {
  local file
  for file in "$CURL_ARGS_DIR"/*; do
    [ -f "$file" ] || continue
    printf '[%s] ' "$(head -c 60 "$file" | tr '\n' ' ')"
  done
}

METADATA=""
for file in "$CURL_ARGS_DIR"/*; do
  [ -f "$file" ] || continue
  content=$(cat "$file")
  case "$content" in
    metadata=*) METADATA=${content#metadata=} ;;
  esac
done

if has_arg '--form-string'; then
  pass "sends metadata with --form-string"
else
  fail "sends metadata with --form-string" \
    "-F truncates any value containing a semicolon; args were: $(args_summary)"
fi

if [ -n "$METADATA" ] && printf '%s' "$METADATA" | jq -e . >/dev/null 2>&1; then
  pass "metadata is valid JSON"
else
  fail "metadata is valid JSON" "got: $METADATA"
fi

CHANGELOG=$(printf '%s' "$METADATA" | jq -r '.changelog // ""' 2>/dev/null)

case "$CHANGELOG" in
  *"$SEMICOLON_TEXT"*) pass "changelog survives a semicolon intact" ;;
  *) fail "changelog survives a semicolon intact" "got: $CHANGELOG" ;;
esac

case "$CHANGELOG" in
  *"$QUOTE_TEXT"*) pass "changelog survives quotes and backslashes" ;;
  *) fail "changelog survives quotes and backslashes" "got: $CHANGELOG" ;;
esac

GAME_VERSIONS=$(printf '%s' "$METADATA" | jq -c '.gameVersions // []' 2>/dev/null)
if [ "$GAME_VERSIONS" = "[16500,11515]" ]; then
  pass "publishes only the type 1 game versions"
else
  fail "publishes only the type 1 game versions" "got: $GAME_VERSIONS"
fi

DISPLAY_NAME=$(printf '%s' "$METADATA" | jq -r '.displayName // ""' 2>/dev/null)
if [ "$DISPLAY_NAME" = "Set Homes V9.9.9" ]; then
  pass "names the file after the version"
else
  fail "names the file after the version" "got: $DISPLAY_NAME"
fi

if has_arg "file=@SetHomes.V9.9.9.jar"; then
  pass "uploads the versioned jar"
else
  fail "uploads the versioned jar" "args were: $(args_summary)"
fi

# The listing this publishes to carries Set Homes v1, which shipped 1.3.1, so a
# 1.x upload would be offered to those servers as an update. The guard runs
# before the first API call, so a refused run records no curl at all.
REFUSE_ARGS_DIR="$WORK/args-refused"
REFUSE_OUTPUT=$(cd "$WORK/run" && PATH="$WORK/bin:$PATH" CURSEFORGE_TOKEN=test-token \
  VERSION=1.2.3 CURL_ARGS_DIR="$REFUSE_ARGS_DIR" bash "$PUBLISH_SH" 2>&1)
REFUSE_STATUS=$?

if [ "$REFUSE_STATUS" -ne 0 ]; then
  pass "refuses to publish a version below 2.0.0"
else
  fail "refuses to publish a version below 2.0.0" "exited 0: $REFUSE_OUTPUT"
fi

if [ ! -d "$REFUSE_ARGS_DIR" ]; then
  pass "calls nothing when it refuses"
else
  fail "calls nothing when it refuses" "recorded: $(ls "$REFUSE_ARGS_DIR")"
fi

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]

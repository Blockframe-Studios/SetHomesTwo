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
QUOTE_TEXT='the "display" name and a C:\\path'

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
printf '%s\n' "$@" > "$CURL_ARGS"
echo '{"id":4242}'
echo 200
STUB
chmod +x "$WORK/bin/curl"

CURL_ARGS="$WORK/args.txt"
export CURL_ARGS

OUTPUT=$(cd "$WORK/run" && PATH="$WORK/bin:$PATH" CURSEFORGE_TOKEN=test-token VERSION=9.9.9 \
  bash "$PUBLISH_SH" 2>&1)
STATUS=$?

if [ "$STATUS" -eq 0 ]; then
  pass "publishes successfully against a stubbed API"
else
  fail "publishes successfully against a stubbed API" "exit $STATUS: $OUTPUT"
fi

if [ ! -f "$CURL_ARGS" ]; then
  fail "calls curl to upload" "no upload call recorded"
  printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
  exit 1
fi

# The metadata argument, without its "metadata=" prefix.
METADATA=$(grep -m1 '^metadata=' "$CURL_ARGS" | sed 's/^metadata=//')

if grep -qx -- '--form-string' "$CURL_ARGS"; then
  pass "sends metadata with --form-string"
else
  fail "sends metadata with --form-string" \
    "-F truncates any value containing a semicolon; args were: $(tr '\n' ' ' < "$CURL_ARGS")"
fi

if [ -n "$METADATA" ] && echo "$METADATA" | jq -e . >/dev/null 2>&1; then
  pass "metadata is valid JSON"
else
  fail "metadata is valid JSON" "got: $METADATA"
fi

CHANGELOG=$(echo "$METADATA" | jq -r '.changelog // ""' 2>/dev/null)

case "$CHANGELOG" in
  *"$SEMICOLON_TEXT"*) pass "changelog survives a semicolon intact" ;;
  *) fail "changelog survives a semicolon intact" "got: $CHANGELOG" ;;
esac

case "$CHANGELOG" in
  *'the "display" name'*) pass "changelog survives double quotes" ;;
  *) fail "changelog survives double quotes" "got: $CHANGELOG" ;;
esac

GAME_VERSIONS=$(echo "$METADATA" | jq -c '.gameVersions // []' 2>/dev/null)
if [ "$GAME_VERSIONS" = "[16500,11515]" ]; then
  pass "publishes only the type 1 game versions"
else
  fail "publishes only the type 1 game versions" "got: $GAME_VERSIONS"
fi

DISPLAY_NAME=$(echo "$METADATA" | jq -r '.displayName // ""' 2>/dev/null)
if [ "$DISPLAY_NAME" = "SetHomesTwo V9.9.9" ]; then
  pass "names the file after the version"
else
  fail "names the file after the version" "got: $DISPLAY_NAME"
fi

if grep -qx -- "file=@SetHomesTwo.V9.9.9.jar" "$CURL_ARGS"; then
  pass "uploads the versioned jar"
else
  fail "uploads the versioned jar" "args were: $(tr '\n' ' ' < "$CURL_ARGS")"
fi

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]

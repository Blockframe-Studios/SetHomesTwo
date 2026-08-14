#!/usr/bin/env bash
# Upload the release jar to BukkitDev.
#
# CurseForge has no game-version range syntax, so 1.21+ is resolved from the
# API at release time rather than hardcoded.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 913275 is set-homes-two. Not 312833 - that is Set Homes v1.
PROJECT_ID=913275
API=https://dev.bukkit.org/api

if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
  echo "CURSEFORGE_TOKEN is not set" >&2
  exit 1
fi

GAME_VERSIONS=$(curl -sS -H "X-Api-Token: $CURSEFORGE_TOKEN" "$API/game/versions" \
  | jq -c '[.[] | select((.name // "") | startswith("1.21")) | .id]')

if [ "$(echo "$GAME_VERSIONS" | jq 'length')" -eq 0 ]; then
  echo "no 1.21 game versions returned by the API" >&2
  exit 1
fi

echo "Publishing to BukkitDev with game version ids: $GAME_VERSIONS"

CHANGELOG=$(bash "$SCRIPT_DIR/release.sh" notes --readme README.md --version "$VERSION")

# --arg JSON-escapes the changelog; it is markdown and may contain quotes,
# backslashes and newlines.
METADATA=$(jq -n \
  --arg changelog "$CHANGELOG" \
  --arg displayName "SetHomesTwo V$VERSION" \
  --argjson gameVersions "$GAME_VERSIONS" \
  '{
    changelog: $changelog,
    changelogType: "markdown",
    displayName: $displayName,
    releaseType: "release",
    gameVersions: $gameVersions
  }')

RESPONSE=$(curl -sS -w '\n%{http_code}' -X POST "$API/projects/$PROJECT_ID/upload-file" \
  -H "X-Api-Token: $CURSEFORGE_TOKEN" \
  -F "metadata=$METADATA" \
  -F "file=@SetHomesTwo.V$VERSION.jar")

BODY=$(echo "$RESPONSE" | head -n -1)
CODE=$(echo "$RESPONSE" | tail -n 1)

echo "$BODY"
if [ "$CODE" -lt 200 ] || [ "$CODE" -ge 300 ]; then
  echo "BukkitDev upload failed with HTTP $CODE" >&2
  exit 1
fi
echo "BukkitDev upload succeeded (HTTP $CODE)"

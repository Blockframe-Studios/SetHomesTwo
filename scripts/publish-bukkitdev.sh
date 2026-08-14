#!/usr/bin/env bash
# Upload the release jar to BukkitDev.
#
# CurseForge has no game-version range syntax, so supported versions are
# resolved from the API at release time. See scripts/game-versions.jq.
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
  | jq -f "$SCRIPT_DIR/game-versions.jq" \
  | jq -c 'map(.id)')

COUNT=$(echo "$GAME_VERSIONS" | jq 'length')
if [ "$COUNT" -eq 0 ]; then
  echo "no supported game versions returned by the API" >&2
  exit 1
fi
# A correct filter returns on the order of 16. A jump to hundreds means the
# type filter stopped matching and a wrong taxonomy is being published.
if [ "$COUNT" -gt 100 ]; then
  echo "$COUNT game versions matched - expected ~16, refusing to publish" >&2
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

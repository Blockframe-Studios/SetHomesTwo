#!/usr/bin/env bash
# Upload the release jar to BukkitDev project 913275 (set-homes-two).
#
# The project id is NOT 312833. That id is the original Set Homes (v1) project,
# and the two are indistinguishable by eye. Verified by following the redirect:
#   /projects/913275 -> /projects/set-homes-two   (this plugin)
#   /projects/312833 -> /projects/set-homes       (v1, do not publish here)
# Re-check the redirect before changing this number.
#
# CurseForge has no game-version range syntax, so the supported versions are
# resolved at release time: every version whose name starts with 1.21. That
# keeps the list correct as Mojang ships patches without editing this file.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

# --arg has jq JSON-escape the changelog for us - it is markdown and may
# contain quotes, backslashes and newlines, so hand-rolled escaping here
# would be exactly the fragile thing to avoid.
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

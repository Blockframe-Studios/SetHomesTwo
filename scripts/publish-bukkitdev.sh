#!/usr/bin/env bash
# Upload the release jar to BukkitDev.
#
# CurseForge has no game-version range syntax, so supported versions are
# resolved from the API at release time. See scripts/game-versions.jq.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 312833 is the Set Homes listing, the one v1 servers already watch for updates.
PROJECT_ID=312833
API=https://dev.bukkit.org/api

if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
  echo "CURSEFORGE_TOKEN is not set" >&2
  exit 1
fi

# This listing carries Set Homes v1, which last shipped 1.3.1, so anything below
# 2.0.0 would reach those servers as an update that reads as a downgrade.
case "$VERSION" in
  0.*|1.*)
    echo "$VERSION is below the 2.0.0 floor for this listing, refusing to publish" >&2
    exit 1
    ;;
esac

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
  --arg displayName "SetHomes V$VERSION" \
  --argjson gameVersions "$GAME_VERSIONS" \
  '{
    changelog: $changelog,
    changelogType: "markdown",
    displayName: $displayName,
    releaseType: "release",
    gameVersions: $gameVersions
  }')

# -F drops everything after a ';' in a value; the changelog contains them.
# The jar still needs -F, for '@'.
RESPONSE=$(curl -sS -w '\n%{http_code}' -X POST "$API/projects/$PROJECT_ID/upload-file" \
  -H "X-Api-Token: $CURSEFORGE_TOKEN" \
  --form-string "metadata=$METADATA" \
  -F "file=@SetHomes-$VERSION.jar")

BODY=$(echo "$RESPONSE" | head -n -1)
CODE=$(echo "$RESPONSE" | tail -n 1)

echo "$BODY"
if [ "$CODE" -lt 200 ] || [ "$CODE" -ge 300 ]; then
  echo "BukkitDev upload failed with HTTP $CODE" >&2
  exit 1
fi
echo "BukkitDev upload succeeded (HTTP $CODE)"

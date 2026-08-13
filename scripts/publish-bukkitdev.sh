#!/usr/bin/env bash
# Upload the release jar to BukkitDev project 312833.
#
# CurseForge has no game-version range syntax, so the supported versions are
# resolved at release time: every version whose name starts with 1.21. That
# keeps the list correct as Mojang ships patches without editing this file.
set -euo pipefail

PROJECT_ID=312833
API=https://dev.bukkit.org/api

if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
  echo "CURSEFORGE_TOKEN is not set" >&2
  exit 1
fi

GAME_VERSIONS=$(curl -sS -H "X-Api-Token: $CURSEFORGE_TOKEN" "$API/game/versions" \
  | python3 -c '
import json, sys
versions = json.load(sys.stdin)
ids = [v["id"] for v in versions if str(v.get("name", "")).startswith("1.21")]
if not ids:
    sys.stderr.write("no 1.21 game versions returned by the API\n")
    sys.exit(1)
print(json.dumps(ids))
')

echo "Publishing to BukkitDev with game version ids: $GAME_VERSIONS"

CHANGELOG=$(python3 - <<'PY'
import io, os, re
text = io.open("README.md", encoding="utf-8").read()
version = os.environ["VERSION"]
match = re.search(r"^#### " + re.escape(version) + r" \(.*?\)\n\n(.*?)(?=\n#### |\Z)", text, re.S | re.M)
print(match.group(1).strip() if match else "")
PY
)

METADATA=$(python3 -c '
import json, os, sys
print(json.dumps({
    "changelog": sys.argv[1],
    "changelogType": "markdown",
    "displayName": "SetHomesTwo V" + os.environ["VERSION"],
    "releaseType": "release",
    "gameVersions": json.loads(sys.argv[2]),
}))
' "$CHANGELOG" "$GAME_VERSIONS")

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

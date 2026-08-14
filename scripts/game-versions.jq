# Supported game versions, filtered from the BukkitDev /game/versions response.
#
# Two numbering schemes are live: the old 1.21.x line, and the calendar scheme
# Minecraft moved to (26.1, 26.2, ...). Both are supported; snapshots are not.
#
# Input:  the full versions array. Output: the matching entries.
[ .[]
  | select(
      (.name // "") as $n
      | ($n | test("^1[.]21([.][0-9]+)?$"))
        or (
          ($n | test("^[0-9]+[.][0-9]+([.][0-9]+)?$"))
          and (($n | split(".") | .[0] | tonumber) >= 26)
        )
    )
]

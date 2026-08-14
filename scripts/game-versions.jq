# Supported game versions, filtered from the BukkitDev /game/versions response.
#
# gameVersionTypeID 1 is the Minecraft version list the project page uses.
# Other type ids carry parallel or unrelated taxonomies - type 3 alone holds
# ~2300 entries, including names like 26.0.0 that are not Minecraft releases.
#
# Within type 1, two numbering schemes are live: the old 1.21.x line and the
# calendar scheme Minecraft moved to (26.1, 26.2, ...). Snapshots are excluded.
#
# This reproduces exactly the 16 versions the 1.2.0 file lists on BukkitDev.
[ .[]
  | select(.gameVersionTypeID == 1)
  | select(
      (.name // "") as $n
      | ($n | test("^1[.]21([.][0-9]+)?$"))
        or (
          ($n | test("^[0-9]+[.][0-9]+([.][0-9]+)?$"))
          and (($n | split(".") | .[0] | tonumber) >= 26)
        )
    )
]

package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface HomesImporter {

    /** The name used as the command argument, e.g. "sethomes". */
    String sourceName();

    /** Scan the source and, unless dryRun, write homes. Never overwrites existing homes. */
    ImportReport run(boolean dryRun);

    static String defaultMaterial() {
        Material material = Material.matchMaterial(ConfigUtil.getConfig().getString("defaultHomeItem", "white_wool"));
        return material == null ? Material.WHITE_WOOL.name() : material.name();
    }

    /**
     * Every name this server has cached, keyed by UUID. Snapshotted once per
     * import because {@link Bukkit#getOfflinePlayers()} scans the playerdata
     * directory on every call.
     */
    static Map<UUID, String> cachedNames() {
        Map<UUID, String> names = new HashMap<>();
        for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
            names.put(candidate.getUniqueId(), candidate.getName());
        }
        return names;
    }
}

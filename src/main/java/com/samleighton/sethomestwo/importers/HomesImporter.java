package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

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
     * The name this server has cached for a player, or null if it has never
     * seen them. Deliberately scans {@link Bukkit#getOfflinePlayers()} rather
     * than calling {@code Bukkit.getOfflinePlayer(UUID)} directly: the latter
     * always returns a non-null object by contract (real Bukkit backs it with
     * usercache.json and reports a null name on a genuine miss, but that
     * distinction should not be relied on for null-safety), and never makes a
     * network call either way, so an import can never block on Mojang.
     */
    static String resolveCachedName(UUID uuid) {
        for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
            if (candidate.getUniqueId().equals(uuid)) {
                return candidate.getName();
            }
        }
        return null;
    }
}

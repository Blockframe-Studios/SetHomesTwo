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
     * Every name this server has cached, snapshotted once so a bulk import
     * doesn't re-scan {@link Bukkit#getOfflinePlayers()} per home - on a
     * server with a large playerdata directory that call is expensive to
     * repeat thousands of times in one command. Deliberately built from
     * {@code getOfflinePlayers()} rather than calling
     * {@code Bukkit.getOfflinePlayer(UUID)} per player: the latter always
     * returns a non-null object by contract, and never makes a network call
     * either way, so an import can never block on Mojang.
     */
    static Map<UUID, String> cachedNames() {
        Map<UUID, String> names = new HashMap<>();
        for (OfflinePlayer candidate : Bukkit.getOfflinePlayers()) {
            names.put(candidate.getUniqueId(), candidate.getName());
        }
        return names;
    }
}

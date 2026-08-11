package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Material;

public interface HomesImporter {

    /** The name used as the command argument, e.g. "sethomes". */
    String sourceName();

    /** Scan the source and, unless dryRun, write homes. Never overwrites existing homes. */
    ImportReport run(boolean dryRun);

    static String defaultMaterial() {
        Material material = Material.matchMaterial(ConfigUtil.getConfig().getString("defaultHomeItem", "white_wool"));
        return material == null ? Material.WHITE_WOOL.name() : material.name();
    }
}

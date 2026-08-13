package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.enums.DebugLevel;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    /**
     * Read live, not cached - a static initialiser bound the value to the
     * first plugin instance loaded, leaving it stale after a /reload.
     */
    public static FileConfiguration getConfig() {
        return SetHomesTwo.instance().getConfig();
    }

    /**
     * @return The configured debug level, defaulting to ERROR
     */
    public static DebugLevel getDebugLevel() {
        return DebugLevel.valueOf(getConfig().getString("debugLevel", DebugLevel.ERROR.name()).toUpperCase());
    }
}

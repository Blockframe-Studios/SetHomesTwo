package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.enums.DebugLevel;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    /**
     * Read from the live plugin on every call. Caching this in a static
     * initialiser bound it to the first plugin instance loaded in the JVM,
     * which left the plugin reading stale values after a /reload.
     *
     * @return The plugin's current configuration
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

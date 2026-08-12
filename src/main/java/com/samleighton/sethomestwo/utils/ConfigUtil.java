package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.enums.DebugLevel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigUtil {

    /**
     * Read from the live plugin on every call. Caching this in a static
     * initialiser bound it to the first plugin instance loaded in the JVM,
     * which left the plugin reading stale values after a /reload.
     * <p>
     * Looked up by name via the plugin manager rather than
     * {@code JavaPlugin.getPlugin(SetHomesTwo.class)}: MockBukkit enables the
     * plugin through a generated subclass loaded by its own classloader, so
     * the literal {@code SetHomesTwo.class} reference never satisfies that
     * method's same-classloader check under test.
     *
     * @return The plugin's current configuration
     */
    public static FileConfiguration getConfig() {
        return ((SetHomesTwo) Bukkit.getPluginManager().getPlugin("SetHomesTwo")).getConfig();
    }

    /**
     * @return The configured debug level, defaulting to ERROR
     */
    public static DebugLevel getDebugLevel() {
        return DebugLevel.valueOf(getConfig().getString("debugLevel", DebugLevel.ERROR.name()).toUpperCase());
    }
}

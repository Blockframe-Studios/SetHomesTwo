package com.samleighton.sethomestwo.support;

import com.samleighton.sethomestwo.SetHomesTwo;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Loads the plugin for tests that have to arrange state before onEnable runs.
 * ServerTestBase cannot do that: it loads the plugin in its own @BeforeEach.
 */
public final class PluginBoot {

    private PluginBoot() {
    }

    /**
     * Loads the plugin with the same two switches ServerTestBase applies, so a
     * drained scheduler can never reach the GitHub API or construct bStats.
     */
    public static SetHomesTwo load() {
        SetHomesTwo plugin = MockBukkit.load(SetHomesTwo.class);
        plugin.getConfig().set("checkForUpdates", false);

        File bStatsDir = new File(plugin.getDataFolder().getParentFile(), "bStats");
        if (!bStatsDir.isDirectory() && !bStatsDir.mkdirs())
            throw new IllegalStateException("could not create " + bStatsDir);
        try {
            Files.writeString(new File(bStatsDir, "config.yml").toPath(), "enabled: false\n");
        } catch (IOException e) {
            throw new IllegalStateException("could not write the bStats opt-out", e);
        }
        return plugin;
    }
}

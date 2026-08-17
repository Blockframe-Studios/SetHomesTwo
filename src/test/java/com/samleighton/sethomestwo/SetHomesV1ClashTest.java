package com.samleighton.sethomestwo;

import com.samleighton.sethomestwo.support.FailOnUnimplemented;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the plugin by hand rather than through ServerTestBase, because the v1
 * plugin has to be registered before our onEnable runs.
 */
@ExtendWith(FailOnUnimplemented.class)
class SetHomesV1ClashTest {

    private ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void refusesToEnableWhenSetHomesV1IsLoaded() {
        MockBukkit.createMockPlugin("SetHomes");

        SetHomesTwo plugin = MockBukkit.load(SetHomesTwo.class);

        assertFalse(plugin.isEnabled(),
                "both jars installed splits /sethome and /homes between the two plugins, so we must not enable");
    }

    @Test
    void theRefusalSaysWhichFileToMoveAndWhatToRunAfterwards() {
        MockBukkit.createMockPlugin("SetHomes");

        String block = severeText(captureLog(() -> MockBukkit.load(SetHomesTwo.class)));

        assertTrue(block.contains("plugins/"), "should name where the old jar is");
        assertTrue(block.contains("/import-homes sethomes"), "should name the command to run once it is gone");
        assertTrue(block.contains("keep"), "should say to keep the old jar so a rollback stays possible");
        assertFalse(block.contains("delete"), "deleting the old jar throws away the rollback");
    }

    @Test
    void aRefusedBootCreatesNothingInOurDataFolder() {
        MockBukkit.createMockPlugin("SetHomes");

        SetHomesTwo plugin = MockBukkit.load(SetHomesTwo.class);

        assertFalse(new File(plugin.getDataFolder(), "config.yml").exists(),
                "the guard must run before initConfig");
        assertFalse(new File(plugin.getDataFolder(), "database").exists(),
                "the guard must run before createDirectories");
    }

    @Test
    void enablesNormallyWhenSetHomesV1IsAbsent() {
        assertTrue(loadPlugin().isEnabled());
    }

    @Test
    void aPluginWhoseNameOnlyStartsWithSetHomesIsNotV1() {
        MockBukkit.createMockPlugin("SetHomesThree");

        assertTrue(loadPlugin().isEnabled());
    }

    @Test
    void theNameMatchIsCaseSensitive() {
        MockBukkit.createMockPlugin("sethomes");

        assertTrue(loadPlugin().isEnabled());
    }

    /**
     * Loads for the cases that expect a normal boot, with the same two switches
     * ServerTestBase applies so a drained scheduler cannot reach the network.
     */
    private SetHomesTwo loadPlugin() {
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

    private List<LogRecord> captureLog(Runnable action) {
        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        Logger logger = Bukkit.getLogger();
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return captured;
    }

    private String severeText(List<LogRecord> records) {
        return records.stream()
                .filter(record -> record.getLevel() == Level.SEVERE)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
    }
}

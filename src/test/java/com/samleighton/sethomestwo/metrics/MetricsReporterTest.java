package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsReporterTest extends ServerTestBase {

    @Test
    void aZeroPluginIdNeverStartsTheReporter() {
        AtomicInteger built = new AtomicInteger();
        MetricsReporter reporter = new MetricsReporter(plugin, 0, () -> true, counters -> {
            built.incrementAndGet();
            return () -> {};
        });

        reporter.startLater();
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);

        assertEquals(0, built.get());
        assertFalse(reporter.isRunning());
    }

    @Test
    void disabledInConfigNeverBuildsTheReporter() {
        AtomicInteger built = new AtomicInteger();
        MetricsReporter reporter = new MetricsReporter(plugin, 1, () -> false, counters -> {
            built.incrementAndGet();
            return () -> {};
        });

        reporter.startLater();
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);

        assertEquals(0, built.get());
        assertFalse(reporter.isRunning());
    }

    @Test
    void enabledInConfigBuildsTheReporterOnceAfterTheDelay() {
        AtomicInteger built = new AtomicInteger();
        MetricsReporter reporter = new MetricsReporter(plugin, 1, () -> true, counters -> {
            built.incrementAndGet();
            return () -> {};
        });

        reporter.startLater();
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS - 1);
        assertFalse(reporter.isRunning());

        server.getScheduler().performTicks(5);
        assertEquals(1, built.get());
        assertTrue(reporter.isRunning());
    }

    @Test
    void theFlagIsReadWhenTheTaskFiresNotWhenScheduled() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        MetricsReporter reporter = new MetricsReporter(plugin, 1, enabled::get, counters -> () -> {});

        reporter.startLater();
        enabled.set(false);
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);

        assertFalse(reporter.isRunning());
    }

    @Test
    void shutdownClosesWhatWasBuilt() {
        AtomicBoolean closed = new AtomicBoolean();
        MetricsReporter reporter = new MetricsReporter(plugin, 1, () -> true, counters -> () -> closed.set(true));

        reporter.startLater();
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);
        reporter.shutdown();

        assertTrue(closed.get());
        assertFalse(reporter.isRunning());
    }

    @Test
    void aFactoryThatThrowsLeavesTheServerAliveAndTheReporterOff() {
        MetricsReporter reporter = new MetricsReporter(plugin, 1, () -> true, counters -> {
            throw new IllegalStateException("relocation check");
        });

        reporter.startLater();
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);

        assertFalse(reporter.isRunning());
        assertTrue(plugin.isEnabled());
    }

    @Test
    void thePluginsOwnReporterIsOffUnderTest() {
        server.getScheduler().performTicks(MetricsReporter.STARTUP_DELAY_TICKS + 5);
        assertFalse(plugin.getMetricsReporter().isRunning());
    }

    @Test
    void theGlobalBStatsSwitchIsReadFromItsOwnConfig(@TempDir File pluginsDir) throws IOException {
        assertTrue(MetricsReporter.bStatsEnabledGlobally(pluginsDir), "no bStats config yet means enabled");

        File bStatsDir = new File(pluginsDir, "bStats");
        assertTrue(bStatsDir.mkdirs());
        File config = new File(bStatsDir, "config.yml");

        Files.writeString(config.toPath(), "enabled: false\n");
        assertFalse(MetricsReporter.bStatsEnabledGlobally(pluginsDir));

        Files.writeString(config.toPath(), "enabled: true\nserverUuid: abc\n");
        assertTrue(MetricsReporter.bStatsEnabledGlobally(pluginsDir));
    }

    @Test
    void theTestHarnessDisablesBStatsThroughItsGlobalSwitch() {
        assertFalse(MetricsReporter.bStatsEnabledGlobally(plugin.getDataFolder().getParentFile()));
    }

    @Test
    void commandChartIdsAreStableAndUnderscored() {
        assertEquals("command_go_home", MetricsReporter.commandChartId("go-home"));
        assertEquals("command_homes", MetricsReporter.commandChartId("homes"));
        assertEquals("command_get_player_homes", MetricsReporter.commandChartId("Get-Player-Homes"));
    }

    @Test
    @SuppressWarnings("deprecation") // Paper deprecates getDescription, but its replacement carries no command list and the reporter itself must stay on the Spigot API.
    void everyDeclaredCommandGetsAChartId() {
        for (String command : plugin.getDescription().getCommands().keySet()) {
            String id = MetricsReporter.commandChartId(command);
            assertTrue(id.matches("command_[a-z_]+"), id);
        }
        assertEquals(14, plugin.getDescription().getCommands().size());
    }
}

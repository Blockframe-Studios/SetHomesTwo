package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.TeleportAttemptsDao;
import com.samleighton.sethomestwo.metrics.UsageCounters;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoHomeTest extends ServerTestBase {

    @BeforeEach
    void disableTeleportSafety() {
        // TeleportSafetyUtil.prefetchChunks calls WorldMock.addPluginChunkTicket,
        // which MockBukkit 4.110.0 does not implement. The safety scan is not what
        // these tests are about, so it is turned off to let the teleport complete.
        plugin.getConfig().set("teleportSafety", false);
    }

    private Map<String, Integer> outcomes() {
        return plugin.getUsageCounters().snapshot(UsageCounters.Family.TELEPORT_OUTCOME);
    }

    @Test
    void consoleIsTurnedAway() {
        assertTrue(server.executeConsole("go-home", "base").hasSucceeded());
        assertTrue(server.getConsoleSender().nextMessage().contains("Only players"));
    }

    @Test
    void tooManyArgumentsAreRejected() {
        TestPlayer player = addTestPlayer("traveller");

        assertTrue(server.execute("go-home", player, "base", "camp").hasSucceeded());

        assertTrue(player.nextMessage().contains("Incorrect number of arguments"));
    }

    @Test
    void aBareCommandTeleportsToTheDefaultHome() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "default", new Location(overworld, 44, 70, 44)));
        plugin.getConfig().set("delay", 0);

        assertTrue(server.execute("go-home", player).hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals(44, player.getLocation().getBlockX());
        assertEquals(44, player.getLocation().getBlockZ());
    }

    @Test
    void aBareCommandWithNoDefaultHomeIsReported() {
        TestPlayer player = addTestPlayer("traveller");
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("go-home", player).hasSucceeded());

        String message = player.nextMessage();
        assertTrue(message.contains("default"), message);
        assertNull(new TeleportAttemptsDao().get(player));
    }

    @Test
    void anUnknownHomeIsNamedInTheError() {
        TestPlayer player = addTestPlayer("traveller");

        assertTrue(server.execute("go-home", player, "nope").hasSucceeded());

        String message = player.nextMessage();
        assertTrue(message.contains("nope"), message);
        assertFalse(message.contains("%s"), message);
    }

    @Test
    void theUnknownHomeMessageIsOverridableInConfig() {
        TestPlayer player = addTestPlayer("traveller");
        plugin.getConfig().set("homeDoesNotExist", "No home called %s here.");

        assertTrue(server.execute("go-home", player, "nope").hasSucceeded());

        String message = player.nextMessage();
        assertTrue(message.contains("No home called nope here."), message);
    }

    @Test
    void withoutPermissionTheCommandIsRefused() {
        TestPlayer player = addTestPlayer("traveller");
        player.addAttachment(plugin, "sh2.go-home", false);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());

        assertTrue(player.nextMessage().contains("do not have permission"));
    }

    @Test
    void anUnknownHomeIsReported() {
        TestPlayer player = addTestPlayer("traveller");
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("go-home", player, "nowhere").hasSucceeded());

        assertTrue(player.nextMessage().contains("does not exist"));
    }

    @Test
    void aBlacklistedHomeRefusesToTeleport() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(overworld.getName());

        Location before = player.getLocation();
        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertTrue(player.nextMessage().contains("cannot teleport to this home"));
        assertEquals(before.getBlockX(), player.getLocation().getBlockX());
        assertNull(new TeleportAttemptsDao().get(player));
    }

    @Test
    void aSecondTeleportWhileOneIsRunningIsRefused() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));
        plugin.getConfig().set("delay", 3);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performOneTick();
        player.nextMessage();

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());

        assertTrue(player.nextMessage().contains("cannot teleport while already teleporting"));
    }

    @Test
    void theCountdownRecordsAnAttemptAndTitlesThePlayer() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));
        plugin.getConfig().set("delay", 3);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performOneTick();

        assertNotNull(new TeleportAttemptsDao().get(player));
        assertEquals(0, player.getLocation().getBlockX());
    }

    @Test
    void thePlayerArrivesOnceTheCountdownCompletes() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));
        plugin.getConfig().set("delay", 0);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals(100, player.getLocation().getBlockX());
        assertEquals(100, player.getLocation().getBlockZ());
        assertNull(new TeleportAttemptsDao().get(player));
    }

    @Test
    void withoutTheTeleportNodeNoRouteToAHomeWorks() {
        TestPlayer player = addPlayer();
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 60, 70, 60)));
        player.addAttachment(plugin, "sh2.teleport", false);
        player.teleport(new Location(overworld, 0, 70, 0));

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertTrue(player.nextMessage().contains("permission"));
        assertEquals(0.0, player.getLocation().getX());
    }

    @Test
    void aCompletedCommandTeleportCountsSourceAndOutcome() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 44, 70, 44)));
        plugin.getConfig().set("delay", 0);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals(1, plugin.getUsageCounters().snapshot(UsageCounters.Family.TELEPORT_SOURCE).get(UsageCounters.SOURCE_COMMAND));
        assertEquals(1, outcomes().get(UsageCounters.OUTCOME_COMPLETED));
        assertEquals(1, outcomes().size());
    }

    @Test
    void movingDuringTheCountdownCountsACancelledTeleport() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));
        plugin.getConfig().set("delay", 3);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performOneTick();
        player.teleport(new Location(overworld, 5, 64, 5));
        server.getScheduler().performTicks(100L);

        assertEquals(1, outcomes().get(UsageCounters.OUTCOME_CANCELLED_MOVED));
        assertFalse(outcomes().containsKey(UsageCounters.OUTCOME_COMPLETED));
    }

    @Test
    void aSecondTeleportWhileOneIsRunningCountsAlreadyTeleporting() {
        TestPlayer player = addTestPlayer("traveller");
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));
        plugin.getConfig().set("delay", 3);

        assertTrue(server.execute("go-home", player, "base").hasSucceeded());
        server.getScheduler().performOneTick();
        assertTrue(server.execute("go-home", player, "base").hasSucceeded());

        assertEquals(1, outcomes().get(UsageCounters.OUTCOME_ALREADY_TELEPORTING));
        assertEquals(2, plugin.getUsageCounters().snapshot(UsageCounters.Family.TELEPORT_SOURCE).get(UsageCounters.SOURCE_COMMAND));
    }
}

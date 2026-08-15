package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.dao.TeleportAttemptsDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.models.TeleportAttempt;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Location;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three bypass nodes, each proven with a holder and a non-holder so the node
 * is what makes the difference rather than the surrounding config.
 */
class BypassNodesTest extends ServerTestBase {

    private static final List<String> NODES = List.of(
            "sh2.bypass-max-homes",
            "sh2.bypass-blacklist",
            "sh2.bypass-teleport-delay"
    );

    @Test
    void aFixturePlayerHoldsNoneOfTheNodes() {
        // All three default to op. An opped fixture would switch off the rules the
        // blacklist and max-homes suites pin, so those tests would pass for the
        // wrong reason.
        TestPlayer player = addPlayer();

        assertFalse(player.isOp(), "a fixture player must not be an operator");
        NODES.forEach(node -> assertFalse(player.hasPermission(node), node));
    }

    @Test
    void theNodesDefaultToOpAndSitInTheAdminBundle() {
        Permission adminBundle = server.getPluginManager().getPermission("sh2.admin");
        assertNotNull(adminBundle);

        for (String node : NODES) {
            Permission permission = server.getPluginManager().getPermission(node);
            assertNotNull(permission, node);
            assertEquals(PermissionDefault.OP, permission.getDefault(), node);
            assertTrue(adminBundle.getChildren().containsKey(node), node);
        }
    }

    @Test
    void aHolderOfTheMaxHomesNodeExceedsTheLimit() {
        limitToOneHome();
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-max-homes", true);
        HomeFixtures.persist(player, "base");

        server.execute("create-home", player, "second").assertSucceeded();

        assertEquals(2, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void withoutTheMaxHomesNodeTheLimitApplies() {
        limitToOneHome();
        TestPlayer player = addPlayer();
        HomeFixtures.persist(player, "base");

        server.execute("create-home", player, "second").assertSucceeded();

        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void aHolderOfTheBlacklistNodeCreatesAHomeInABlacklistedWorld() {
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-blacklist", true);
        HomeFixtures.blacklist(nether.getName());
        player.teleport(new Location(nether, 10, 70, 10));

        server.execute("create-home", player, "base").assertSucceeded();

        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void withoutTheBlacklistNodeCreatingInABlacklistedWorldIsRefused() {
        TestPlayer player = addPlayer();
        HomeFixtures.blacklist(nether.getName());
        player.teleport(new Location(nether, 10, 70, 10));

        server.execute("create-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("blacklisted"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aHolderOfTheBlacklistNodeMovesAHomeIntoABlacklistedWorld() {
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-blacklist", true);
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(nether.getName());
        player.teleport(new Location(nether, 10, 70, 10));

        server.execute("move-home", player, "base").assertSucceeded();

        Home moved = new HomesDao(true).getAll(player.getUniqueId()).get(0);
        assertEquals(nether.getUID().toString(), moved.getWorld());
        assertEquals(10.0, moved.getX());
    }

    @Test
    void withoutTheBlacklistNodeMovingIntoABlacklistedWorldIsRefused() {
        TestPlayer player = addPlayer();
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(nether.getName());
        player.teleport(new Location(nether, 10, 70, 10));

        server.execute("move-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("blacklisted"));
        assertEquals(overworld.getUID().toString(),
                new HomesDao(true).getAll(player.getUniqueId()).get(0).getWorld());
    }

    @Test
    void aHolderOfTheBlacklistNodeTeleportsToAHomeInABlacklistedWorld() {
        disableTeleportSafety();
        plugin.getConfig().set("delay", 0);
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-blacklist", true);
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "far", new Location(nether, 100, 70, 100)));
        HomeFixtures.blacklist(nether.getName());

        server.execute("go-home", player, "far").assertSucceeded();
        server.getScheduler().performTicks(100L);

        assertEquals(nether.getName(), player.getWorld().getName());
        assertEquals(100, player.getLocation().getBlockX());
    }

    @Test
    void withoutTheBlacklistNodeTheHomeStaysUnreachable() {
        disableTeleportSafety();
        plugin.getConfig().set("delay", 0);
        TestPlayer player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "far", new Location(nether, 100, 70, 100)));
        HomeFixtures.blacklist(nether.getName());

        server.execute("go-home", player, "far").assertSucceeded();
        server.getScheduler().performTicks(100L);

        assertEquals(overworld.getName(), player.getWorld().getName());
        assertEquals(0, player.getLocation().getBlockX());
    }

    @Test
    void aHolderOfTheDelayNodeArrivesWithoutWaitingOutTheCountdown() {
        disableTeleportSafety();
        plugin.getConfig().set("delay", 3);
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-teleport-delay", true);
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));

        server.execute("go-home", player, "base").assertSucceeded();
        server.getScheduler().performOneTick();

        assertEquals(100, player.getLocation().getBlockX());
    }

    @Test
    void withoutTheDelayNodeTheCountdownStillRuns() {
        disableTeleportSafety();
        plugin.getConfig().set("delay", 3);
        TestPlayer player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(HomeFixtures.home(player, "base", new Location(overworld, 100, 70, 100)));

        server.execute("go-home", player, "base").assertSucceeded();
        server.getScheduler().performOneTick();

        assertEquals(0, player.getLocation().getBlockX());
    }

    @Test
    void aHolderOfTheDelayNodeIsNotCancelledByMoving() {
        TestPlayer player = addPlayer();
        player.addAttachment(plugin, "sh2.bypass-teleport-delay", true);
        player.teleport(new Location(overworld, 0, 64, 0));

        Dao<TeleportAttempt> attempts = new TeleportAttemptsDao();
        attempts.save(new TeleportAttempt(player, player.getLocation()));
        player.teleport(new Location(overworld, 20, 64, 20));

        TeleportAttempt attempt = attempts.get(player);
        assertNotNull(attempt);
        assertTrue(attempt.canTeleport());
    }

    @Test
    void withoutTheDelayNodeMovingCancelsTheAttempt() {
        TestPlayer player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));

        Dao<TeleportAttempt> attempts = new TeleportAttemptsDao();
        attempts.save(new TeleportAttempt(player, player.getLocation()));
        player.teleport(new Location(overworld, 20, 64, 20));

        TeleportAttempt attempt = attempts.get(player);
        assertNotNull(attempt);
        assertFalse(attempt.canTeleport());
    }

    private void limitToOneHome() {
        plugin.getConfig().set("maxHomeEnabled", true);
        plugin.getConfig().set("maxHomesType", "singular");
        plugin.getConfig().set("maxHomes", 1);
    }

    /**
     * TeleportSafetyUtil.prefetchChunks calls WorldMock.addPluginChunkTicket, which
     * MockBukkit 4.110.0 does not implement.
     */
    private void disableTeleportSafety() {
        plugin.getConfig().set("teleportSafety", false);
    }
}

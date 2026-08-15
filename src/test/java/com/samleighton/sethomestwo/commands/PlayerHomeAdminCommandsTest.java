package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.tabcompleters.PlayerHomesTabCompleter;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHomeAdminCommandsTest extends ServerTestBase {

    @Test
    void deletingAnOfflinePlayersHomeWorks() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        UUID targetId = target.getUniqueId();
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        server.execute("delete-player-home", admin, "Steve", "base").assertSucceeded();

        assertTrue(new HomesDao(true).getAll(targetId).isEmpty());
    }

    @Test
    void movingAnOfflinePlayersHomeUsesTheAdminLocation() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        UUID targetId = target.getUniqueId();
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.move-player-home", true);
        admin.teleport(new Location(overworld, 250, 70, 250));

        server.execute("move-player-home", admin, "Steve", "base").assertSucceeded();

        assertEquals(250.0, new HomesDao(true).getAll(targetId).get(0).getX());
    }

    @Test
    void teleportingToAnOfflinePlayersHomeIsAccepted() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(target, "base", new Location(overworld, 30, 70, 30)));
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.go-player-home", true);
        admin.addAttachment(plugin, "sh2.teleport", true);
        admin.teleport(new Location(overworld, 0, 64, 0));
        plugin.getConfig().set("delay", 0);
        // TeleportSafetyUtil.prefetchChunks reaches an unimplemented MockBukkit call.
        plugin.getConfig().set("teleportSafety", false);

        server.execute("go-player-home", admin, "Steve", "base").assertSucceeded();
        server.getScheduler().performTicks(100L);

        assertEquals(30.0, admin.getLocation().getX());
    }

    @Test
    void anUnknownPlayerIsReported() {
        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        server.execute("delete-player-home", admin, "Nobody", "base").assertSucceeded();

        assertTrue(admin.nextMessage().contains("No player by that name"));
    }

    @Test
    void eachCommandIsRefusedWithoutItsNodeAtTheCommandGate() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", false);

        server.execute("delete-player-home", admin, "Steve", "base").assertSucceeded();

        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theInCodeGuardAlsoRefuses() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", false);

        // Call the executor directly. Going through server.execute would be
        // refused by plugin.yml's permission before onCommand is reached.
        new DeletePlayerHome().onCommand(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"Steve", "base"});

        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theV1AliasesWork() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        server.execute("delhome-of", admin, "Steve", "base").assertSucceeded();

        assertTrue(new HomesDao(true).getAll(target.getUniqueId()).isEmpty());
    }

    @Test
    void theWrongNumberOfArgumentsShowsTheUsage() {
        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.move-player-home", true);

        server.execute("move-player-home", admin, "Steve").assertSucceeded();

        assertTrue(admin.nextMessage().contains("Incorrect number of arguments"));
        assertTrue(admin.nextMessage().contains("Usage: /move-player-home <player> <home>"));
    }

    @Test
    void theTabCompleterOffersOnlinePlayerNamesFirst() {
        addPlayer("Steve");
        PlayerMock admin = addPlayer("Admin");

        List<String> completions = new PlayerHomesTabCompleter().onTabComplete(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"St"});

        assertTrue(completions.contains("Steve"));
        assertFalse(completions.contains("Admin"));
    }

    @Test
    void theTabCompleterOffersTheTargetsHomeNamesSecond() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        HomeFixtures.persist(target, "mine");
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");

        List<String> completions = new PlayerHomesTabCompleter().onTabComplete(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"Steve", "b"});

        assertEquals(List.of("base"), completions);
    }

    @Test
    void theInCodeGuardAlsoRefusesTheOtherTwoCommands() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        double beforeX = new HomesDao(true).getAll(target.getUniqueId()).get(0).getX();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.go-player-home", false);
        admin.addAttachment(plugin, "sh2.move-player-home", false);
        admin.teleport(new Location(overworld, 900, 70, 900));
        // So a regression that lets the teleport through fails on the assertion
        // below rather than aborting on an unimplemented mock call.
        plugin.getConfig().set("teleportSafety", false);

        new GoPlayerHome().onCommand(admin,
                Objects.requireNonNull(plugin.getCommand("go-player-home")),
                "go-player-home", new String[]{"Steve", "base"});
        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(900.0, admin.getLocation().getX());

        new MovePlayerHome().onCommand(admin,
                Objects.requireNonNull(plugin.getCommand("move-player-home")),
                "move-player-home", new String[]{"Steve", "base"});
        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(beforeX, new HomesDao(true).getAll(target.getUniqueId()).get(0).getX());
    }

    @Test
    void anUnknownHomeIsReportedByEachCommand() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);
        admin.addAttachment(plugin, "sh2.move-player-home", true);
        admin.addAttachment(plugin, "sh2.go-player-home", true);

        server.execute("delete-player-home", admin, "Steve", "nope").assertSucceeded();
        assertTrue(admin.nextMessage().contains("no longer exists"));

        server.execute("move-player-home", admin, "Steve", "nope").assertSucceeded();
        assertTrue(admin.nextMessage().contains("no longer exists"));

        server.execute("go-player-home", admin, "Steve", "nope").assertSucceeded();
        assertTrue(admin.nextMessage().contains("no longer exists"));

        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theSuccessMessageNamesTheOwnerAndHomeCanonically() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        // Both names typed in the wrong case. The reply must echo the stored
        // spelling, not what was typed.
        server.execute("delete-player-home", admin, "sTeVe", "BaSe").assertSucceeded();

        String reply = admin.nextMessage();
        assertTrue(reply.contains("Steve"), reply);
        assertTrue(reply.contains("base"), reply);
    }
}

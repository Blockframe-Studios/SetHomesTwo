package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveHomeTest extends ServerTestBase {

    @Test
    void theHomeMovesToThePlayersLocation() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        player.teleport(new Location(overworld, 100, 70, -40));

        server.execute("move-home", player, "base").assertSucceeded();

        Home moved = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(100.0, moved.getX());
        assertEquals(-40.0, moved.getZ());
    }

    @Test
    void anUnknownHomeIsReported() {
        PlayerMock player = addPlayer();

        server.execute("move-home", player, "nope").assertSucceeded();

        String message = player.nextMessage();
        assertTrue(message.contains("nope"), message);
        assertFalse(message.contains("%s"), message);
    }

    @Test
    void withoutPermissionTheCommandIsRefused() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        player.addAttachment(plugin, "sh2.move-home", false);
        Location before = player.getLocation();
        player.teleport(new Location(overworld, 100, 70, -40));

        server.execute("move-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("permission"));
        assertEquals(before.getX(), new HomesDao().getAll(player.getUniqueId()).get(0).getX());
    }

    @Test
    void movingIntoABlacklistedWorldIsRefused() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist("world_nether");
        player.teleport(new Location(nether, 10, 70, 10));

        server.execute("move-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("blacklisted"));
    }

    @Test
    void theAliasWorks() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        player.teleport(new Location(overworld, 5, 70, 5));

        server.execute("uhome", player, "base").assertSucceeded();

        assertEquals(5.0, new HomesDao().getAll(player.getUniqueId()).get(0).getX());
    }

    @Test
    void movingAcrossWorldsRewritesTheWorldAndDimension() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        player.teleport(new Location(nether, 8, 70, 8));

        server.execute("move-home", player, "base").assertSucceeded();

        Home moved = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(nether.getUID().toString(), moved.getWorld());
        assertEquals("NETHER", moved.getDimension());
    }

    @Test
    void theWrongNumberOfArgumentsShowsTheUsage() {
        PlayerMock player = addPlayer();

        server.execute("move-home", player).assertSucceeded();

        assertTrue(player.nextMessage().contains("Incorrect number of arguments"));
        assertTrue(player.nextMessage().contains("Usage: /move-home <name>"));
    }

    @Test
    void theUsageNamesTheAliasThatWasTyped() {
        PlayerMock player = addPlayer();

        server.dispatchCommand(player, "uhome");

        player.nextMessage();
        assertTrue(player.nextMessage().contains("Usage: /uhome <name>"));
    }
}

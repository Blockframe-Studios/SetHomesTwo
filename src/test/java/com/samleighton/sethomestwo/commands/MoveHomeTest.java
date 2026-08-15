package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertTrue(player.nextMessage().contains("no longer exists"));
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
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateHomeTest extends ServerTestBase {

    @Test
    void aHomeIsCreatedAtThePlayersLocation() {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(overworld, 12, 65, -8));

        server.execute("create-home", player, "base").assertSucceeded();

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("base", homes.get(0).getName());
        assertEquals(12.0, homes.get(0).getX());
    }

    @Test
    void missingNameIsRejected() {
        PlayerMock player = server.addPlayer();

        server.execute("create-home", player).assertSucceeded();

        assertTrue(player.nextMessage().contains("Incorrect number of arguments"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aDuplicateNameIsRejected() {
        PlayerMock player = server.addPlayer();
        HomeFixtures.persist(player, "base");

        server.execute("create-home", player, "BASE").assertSucceeded();

        assertTrue(player.nextMessage().contains("You already have a home called"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void anInvalidMaterialIsRejected() {
        PlayerMock player = server.addPlayer();

        server.execute("create-home", player, "base", "not_a_material").assertSucceeded();

        assertTrue(player.nextMessage().contains("not valid"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aSuppliedMaterialIsStored() {
        PlayerMock player = server.addPlayer();

        server.execute("create-home", player, "base", "diamond").assertSucceeded();

        assertEquals(Material.DIAMOND.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void aBlacklistedDimensionIsRejected() {
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.blacklist(overworld.getName());

        server.execute("create-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("You cannot set a home in this dimension"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void theSingularMaxHomesLimitIsEnforced() {
        PlayerMock player = server.addPlayer();
        plugin.getConfig().set("maxHomeEnabled", true);
        plugin.getConfig().set("maxHomesType", "singular");
        plugin.getConfig().set("maxHomes", 1);

        HomeFixtures.persist(player, "base");

        server.execute("create-home", player, "camp").assertSucceeded();

        assertTrue(player.nextMessage().contains("maximum number of homes"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void groupLimitsAreSkippedWhenLuckPermsIsAbsent() {
        PlayerMock player = server.addPlayer();
        plugin.getConfig().set("maxHomeEnabled", true);
        plugin.getConfig().set("maxHomesType", "groups");

        HomeFixtures.persist(player, "base");

        // LuckPerms is a soft dependency and is not installed here, so the
        // guard short-circuits and the home is still created.
        server.execute("create-home", player, "camp").assertSucceeded();

        assertEquals(2, new HomesDao().getAll(player.getUniqueId()).size());
    }
}

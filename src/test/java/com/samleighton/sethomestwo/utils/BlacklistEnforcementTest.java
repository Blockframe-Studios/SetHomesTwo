package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacklistEnforcementTest extends ServerTestBase {

    @Test
    void aConventionalWorldStillEnforces() {
        HomeFixtures.blacklist("world_nether");

        assertTrue(ServerUtil.isWorldBlacklisted(nether));
        assertFalse(ServerUtil.isWorldBlacklisted(overworld));
    }

    @Test
    void aFourthWorldEnforces() {
        WorldMock creative = server.addSimpleWorld("creative");
        creative.setEnvironment(World.Environment.NORMAL);
        HomeFixtures.blacklist("creative");

        // The positional map could never address a fourth world, so this is the
        // regression test for the silent no-op.
        assertTrue(ServerUtil.isWorldBlacklisted(creative));
        assertFalse(ServerUtil.isWorldBlacklisted(overworld));
    }

    @Test
    void aSecondOverworldIsBlacklistedIndependently() {
        WorldMock resource = server.addSimpleWorld("resource");
        resource.setEnvironment(World.Environment.NORMAL);
        HomeFixtures.blacklist("resource");

        assertTrue(ServerUtil.isWorldBlacklisted(resource));
        assertFalse(ServerUtil.isWorldBlacklisted(overworld));
    }

    @Test
    void creatingAHomeInABlacklistedFourthWorldIsRefused() {
        WorldMock creative = server.addSimpleWorld("creative");
        creative.setEnvironment(World.Environment.NORMAL);
        HomeFixtures.blacklist("creative");

        PlayerMock player = addPlayer();
        player.teleport(new Location(creative, 0, 64, 0));

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

        assertTrue(player.nextMessage().contains("blacklisted"));
        assertTrue(new com.samleighton.sethomestwo.dao.HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aHomeInABlacklistedFourthWorldCannotBeTeleportedTo() {
        WorldMock creative = server.addSimpleWorld("creative");
        creative.setEnvironment(World.Environment.NORMAL);

        PlayerMock player = addPlayer();
        HomeFixtures.persist(HomeFixtures.home(player, "far", new Location(creative, 0, 64, 0)));
        HomeFixtures.blacklist("creative");

        assertFalse(new com.samleighton.sethomestwo.dao.HomesDao()
                .getAll(player.getUniqueId()).get(0).getCanTeleport());
    }
}

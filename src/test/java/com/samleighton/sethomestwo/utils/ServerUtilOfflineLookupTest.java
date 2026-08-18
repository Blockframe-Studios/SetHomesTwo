package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerUtilOfflineLookupTest extends ServerTestBase {

    @Test
    void anOnlinePlayerStillResolves() {
        PlayerMock player = addPlayer("Steve");

        assertEquals(player.getUniqueId().toString(), ServerUtil.getPlayerUUID("Steve"));
    }

    @Test
    void anOfflinePlayerWithStoredHomesResolves() {
        PlayerMock player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");
        String expected = player.getUniqueId().toString();

        player.disconnect();

        assertEquals(expected, ServerUtil.getPlayerUUID("Steve"));
    }

    @Test
    void anOnlinePlayerResolvesRegardlessOfCase() {
        PlayerMock player = addPlayer("Steve");

        assertEquals(player.getUniqueId().toString(), ServerUtil.getPlayerUUID("steve"));
    }

    @Test
    void anOfflinePlayerResolvesRegardlessOfCase() {
        PlayerMock player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");
        String expected = player.getUniqueId().toString();

        player.disconnect();

        assertEquals(expected, ServerUtil.getPlayerUUID("sTeVe"));
    }

    @Test
    void aPlayerWithNoHomesAndNoSessionDoesNotResolve() {
        assertNull(ServerUtil.getPlayerUUID("Nobody"));
    }
}

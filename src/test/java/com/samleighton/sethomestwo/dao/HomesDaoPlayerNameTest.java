package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HomesDaoPlayerNameTest extends ServerTestBase {

    @Test
    void theOwnerNameIsStoredWhenAHomeIsSaved() {
        PlayerMock player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");

        assertEquals("Steve", new HomesDao().getAll(player.getUniqueId()).get(0).getPlayerName());
    }

    @Test
    void aNameResolvesToItsOwnersUuid() {
        PlayerMock player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");

        assertEquals(player.getUniqueId().toString(), new HomesDao().uuidForName("Steve"));
    }

    @Test
    void anUnknownNameResolvesToNull() {
        assertNull(new HomesDao().uuidForName("Nobody"));
    }

    @Test
    void aRenameIsPickedUpByRefresh() {
        PlayerMock player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");

        new HomesDao().refreshPlayerName(player.getUniqueId(), "Steven");

        assertEquals(player.getUniqueId().toString(), new HomesDao().uuidForName("Steven"));
        assertNull(new HomesDao().uuidForName("Steve"));
    }
}

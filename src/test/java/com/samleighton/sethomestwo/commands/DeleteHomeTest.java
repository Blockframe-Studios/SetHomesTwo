package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteHomeTest extends ServerTestBase {

    @Test
    void consoleIsTurnedAway() {
        assertTrue(server.executeConsole("delete-home", "base").hasSucceeded());
        assertTrue(server.getConsoleSender().nextMessage().contains("Only players"));
    }

    @Test
    void wrongArgumentCountReportsUsage() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("delete-home", player).hasSucceeded());

        assertTrue(player.nextMessage().contains("Incorrect number of arguments"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void anUnknownHomeIsReportedAndNothingIsDeleted() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("delete-home", player, "nowhere").hasSucceeded());

        assertTrue(player.nextMessage().contains("You do not have a home by the name"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void aHomeIsDeleted() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        HomeFixtures.persist(player, "camp");

        assertTrue(server.execute("delete-home", player, "base").hasSucceeded());

        List<Home> remaining = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, remaining.size());
        assertEquals("camp", remaining.get(0).getName());
    }

    @Test
    void withTwoHomesSharingANameOnlyOneIsDeleted() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("delete-home", player, "base").hasSucceeded());

        // The 1.2.0 behaviour change: previously this removed every matching row.
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }
}

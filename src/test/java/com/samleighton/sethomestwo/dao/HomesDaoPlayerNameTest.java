package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void initTablesIsSafeToRunAgainAfterTheColumnAlreadyExists() {
        Connection connection = plugin.getConnectionManager().getConnection("homes");

        assertTrue(DatabaseUtil.initTables(connection));
        assertEquals(1, countPlayerNameColumns(connection));
    }

    private int countPlayerNameColumns(Connection connection) {
        int count = 0;

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("pragma table_info(players_homes);")) {
            while (rs.next()) {
                if ("player_name".equalsIgnoreCase(rs.getString("name"))) count++;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read players_homes columns", e);
        }

        return count;
    }
}

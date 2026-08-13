package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomesDaoTest extends ServerTestBase {

    @Test
    void savedHomeComesBackFromGetAll() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        List<Home> homes = new HomesDao().getAll(player.getUniqueId());

        assertEquals(1, homes.size());
        assertEquals("base", homes.get(0).getName());
        assertNotNull(homes.get(0).getId());
    }

    @Test
    void getReturnsNullForUnknownName() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        assertNull(new HomesDao().get(player.getUniqueId(), "nowhere"));
    }

    @Test
    void getByIdIsScopedToTheOwningPlayer() {
        PlayerMock owner = addPlayer();
        PlayerMock stranger = addPlayer();
        Home home = HomeFixtures.persist(owner, "base");

        HomesDao dao = new HomesDao();

        assertNotNull(dao.getById(owner.getUniqueId(), home.getId()));
        assertNull(dao.getById(stranger.getUniqueId(), home.getId()));
    }

    @Test
    void deleteRefusesAHomeWithNoId() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        Home unsaved = HomeFixtures.home(player, "base");
        HomesDao dao = new HomesDao();

        List<LogRecord> logged = captureLog(() -> assertFalse(dao.delete(unsaved)));

        assertEquals(1, dao.getAll(player.getUniqueId()).size());
        assertTrue(loggedSevere(logged, "Refusing to delete a home that has no id."),
                "Expected the no-id guard to log a SEVERE refusal message");
    }

    @Test
    void deleteRemovesExactlyOneRowWhenTwoHomesShareAName() {
        PlayerMock player = addPlayer();
        Home first = HomeFixtures.persist(player, "base");
        Home second = HomeFixtures.persist(player, "base");

        HomesDao dao = new HomesDao();
        assertTrue(dao.delete(first));

        List<Home> remaining = dao.getAll(player.getUniqueId());
        assertEquals(1, remaining.size());
        assertEquals(second.getId(), remaining.get(0).getId());
    }

    @Test
    void updateRefusesAHomeWithNoId() {
        PlayerMock player = addPlayer();
        Home unsaved = HomeFixtures.home(player, "base");
        HomesDao dao = new HomesDao();

        List<LogRecord> logged = captureLog(() -> assertFalse(dao.update(unsaved)));

        assertTrue(loggedSevere(logged, "Refusing to update a home that has no id."),
                "Expected the no-id guard to log a SEVERE refusal message");
    }

    @Test
    void updatePersistsNameMaterialAndLocation() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        home.setName("camp");
        home.setMaterial(Material.DIAMOND.name());
        home.setX(101.5);
        home.setY(72.0);
        home.setZ(-33.25);

        HomesDao dao = new HomesDao();
        assertTrue(dao.update(home));

        Home reloaded = dao.getById(player.getUniqueId(), home.getId());
        assertNotNull(reloaded);
        assertEquals("camp", reloaded.getName());
        assertEquals(Material.DIAMOND.name(), reloaded.getMaterial());
        assertEquals(101.5, reloaded.getX());
        assertEquals(72.0, reloaded.getY());
        assertEquals(-33.25, reloaded.getZ());
    }

    @Test
    void nameExistsIsCaseInsensitive() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "Base");

        assertTrue(new HomesDao().nameExists(player.getUniqueId(), "bAsE", null));
    }

    @Test
    void nameExistsIsFalseForAFreeName() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        assertFalse(new HomesDao().nameExists(player.getUniqueId(), "camp", null));
    }

    @Test
    void nameExistsIgnoresTheHomeBeingRenamed() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        // Renaming a home to the name it already has is not a conflict.
        assertFalse(new HomesDao().nameExists(player.getUniqueId(), "base", home.getId()));
    }

    @Test
    void nameExistsStillCatchesAnotherHomesName() {
        PlayerMock player = addPlayer();
        Home first = HomeFixtures.persist(player, "base");
        HomeFixtures.persist(player, "camp");

        assertTrue(new HomesDao().nameExists(player.getUniqueId(), "camp", first.getId()));
    }

    @Test
    void nameExistsDoesNotLeakAcrossPlayers() {
        PlayerMock owner = addPlayer();
        PlayerMock stranger = addPlayer();
        HomeFixtures.persist(owner, "base");

        assertFalse(new HomesDao().nameExists(stranger.getUniqueId(), "base", null));
    }

    @Test
    void blacklistedDimensionBlocksTeleportForPlayers() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(overworld.getName());

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);

        assertFalse(home.getCanTeleport());
        assertEquals("Cannot teleport here: dimension blacklisted", home.getDescription());
    }

    @Test
    void blacklistedDimensionStillAllowsTheAdminView() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(overworld.getName());

        Home home = new HomesDao(true).getAll(player.getUniqueId()).get(0);

        assertTrue(home.getCanTeleport());
    }

    /**
     * Captures what gets logged during {@code action}. The handler is always
     * removed afterward so it cannot leak into other tests.
     */
    private List<LogRecord> captureLog(Runnable action) {
        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        Logger logger = Bukkit.getLogger();
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }

        return captured;
    }

    private boolean loggedSevere(List<LogRecord> records, String message) {
        return records.stream().anyMatch(
                record -> record.getLevel() == Level.SEVERE && message.equals(record.getMessage()));
    }
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateHomeTest extends ServerTestBase {

    @Test
    void aHomeIsCreatedAtThePlayersLocation() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 12, 65, -8));

        server.execute("create-home", player, "base").assertSucceeded();

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("base", homes.get(0).getName());
        assertEquals(12.0, homes.get(0).getX());
    }

    @Test
    void missingNameIsRejected() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player).assertSucceeded();

        assertTrue(player.nextMessage().contains("Incorrect number of arguments"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aDuplicateNameIsRejected() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        server.execute("create-home", player, "BASE").assertSucceeded();

        assertTrue(player.nextMessage().contains("You already have a home called"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void anInvalidMaterialIsRejected() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "not_a_material").assertSucceeded();

        assertTrue(player.nextMessage().contains("not valid"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aSuppliedMaterialIsStored() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "diamond").assertSucceeded();

        assertEquals(Material.DIAMOND.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void aBlacklistedDimensionIsRejected() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.blacklist(overworld.getName());

        server.execute("create-home", player, "base").assertSucceeded();

        assertTrue(player.nextMessage().contains("You cannot set a home in this dimension"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void theSingularMaxHomesLimitIsEnforced() {
        PlayerMock player = addPlayer();
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
        PlayerMock player = addPlayer();
        plugin.getConfig().set("maxHomeEnabled", true);
        plugin.getConfig().set("maxHomesType", "groups");

        HomeFixtures.persist(player, "base");

        // LuckPerms is a soft dependency and is not installed here, so the
        // guard should short-circuit - logging a distinctive warning - and
        // the home should still be created. Asserting only the creation
        // count would also pass if maxHomeEnabled/maxHomesType were ignored
        // for an unrelated reason, so assert the warning too, proving the
        // groups branch was actually reached.
        List<LogRecord> logged = captureLog(() ->
                server.execute("create-home", player, "camp").assertSucceeded());

        assertEquals(2, new HomesDao().getAll(player.getUniqueId()).size());
        assertTrue(loggedWarning(logged,
                        "maxHomesType is 'groups' but LuckPerms is not installed. Max homes limit will not be enforced."),
                "Expected the groups-limit guard to log a warning when LuckPerms is absent");
    }

    /**
     * Attach a temporary handler to the Bukkit logger for the duration of
     * {@code action}, so a test can assert on what got logged rather than
     * only on a method's return value. The handler is always removed
     * afterward so it cannot leak into other tests. Mirrors the helper in
     * HomesDaoTest; not shared because it is only two call sites in
     * different classes.
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

    private boolean loggedWarning(List<LogRecord> records, String message) {
        return records.stream().anyMatch(
                record -> record.getLevel() == Level.WARNING && message.equals(record.getMessage()));
    }
}

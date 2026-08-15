package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void aBareCommandCreatesTheDefaultHome() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 7, 65, 7));

        server.execute("create-home", player).assertSucceeded();

        List<Home> homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("default", homes.get(0).getName());
        assertEquals(7.0, homes.get(0).getX());
        assertEquals(Material.WHITE_WOOL.name(), homes.get(0).getMaterial());
        assertNull(homes.get(0).getDescription());
    }

    @Test
    void theNameDefaultBehavesLikeTheBareCommand() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "default").assertSucceeded();
        player.nextMessage();
        server.execute("create-home", player).assertSucceeded();

        assertTrue(player.nextMessage().contains("You already have a home called"));
        List<Home> homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("default", homes.get(0).getName());
    }

    /**
     * Argument 2 'default' is the icon sentinel, not the default home name. The
     * two meanings live in different argument positions and must not be merged.
     */
    @Test
    void theIconSentinelNeverBecomesTheHomeName() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "default").assertSucceeded();

        List<Home> homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("base", homes.get(0).getName());
        assertEquals(Material.WHITE_WOOL.name(), homes.get(0).getMaterial());
        assertNull(homes.get(0).getDescription());
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
    void aNonMaterialSecondArgumentBecomesTheDescription() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "my", "main", "base").assertSucceeded();

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("my main base", homes.get(0).getDescription());
        assertEquals(Material.WHITE_WOOL.name(), homes.get(0).getMaterial());
    }

    @Test
    void aMaterialSecondArgumentStillSetsTheIcon() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "diamond_block", "my", "base").assertSucceeded();

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(Material.DIAMOND_BLOCK.name(), homes.get(0).getMaterial());
        assertEquals("my base", homes.get(0).getDescription());
    }

    @Test
    void theChosenIconIsNamedInTheSuccessMessage() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "diamond_block").assertSucceeded();

        assertTrue(player.nextMessage().contains("DIAMOND_BLOCK"));
    }

    /**
     * Pins the trade-off the forgiving parsing introduces, so it stays a
     * documented behaviour rather than a surprise. Looks like a duplicate of
     * aMaterialSecondArgumentStillSetsTheIcon; it is not, keep both.
     */
    @Test
    void aDescriptionBeginningWithAMaterialWordLosesThatWordToTheIcon() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "stone", "house").assertSucceeded();

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(Material.STONE.name(), home.getMaterial());
        assertEquals("house", home.getDescription());
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

        // The count alone would pass even if the groups branch were never reached.
        List<LogRecord> logged = captureLog(() ->
                server.execute("create-home", player, "camp").assertSucceeded());

        assertEquals(2, new HomesDao().getAll(player.getUniqueId()).size());
        assertTrue(loggedWarning(logged,
                        "maxHomesType is 'groups' but LuckPerms is not installed. Max homes limit will not be enforced."),
                "Expected the groups-limit guard to log a warning when LuckPerms is absent");
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

    private boolean loggedWarning(List<LogRecord> records, String message) {
        return records.stream().anyMatch(
                record -> record.getLevel() == Level.WARNING && message.equals(record.getMessage()));
    }

    @Test
    void aValidButNonItemMaterialWordIsDescriptionText() {
        PlayerMock player = addPlayer();

        // water names a real Material but not an item. Storing it as the icon
        // would make HomesGui throw on new ItemStack and the menu stop opening.
        server.execute("create-home", player, "base", "water", "front").assertSucceeded();

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(Material.WHITE_WOOL.name(), home.getMaterial());
        assertEquals("water front", home.getDescription());
    }

    @Test
    void theHomesMenuStillOpensAfterAHomeNamedAfterANonItem() {
        PlayerMock player = addPlayer();
        server.execute("create-home", player, "base", "water", "front").assertSucceeded();

        assertDoesNotThrow(() -> server.execute("homes", player).assertSucceeded());
    }

    /**
     * Reachable by typing a double space, which Bukkit turns into an empty
     * argument rather than dropping it.
     */
    @Test
    void anEmptySecondArgumentDoesNotLeakIntoTheDescription() {
        PlayerMock player = addPlayer();

        server.dispatchCommand(player, "create-home base  hi");

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(Material.WHITE_WOOL.name(), home.getMaterial());
        assertEquals("hi", home.getDescription());
    }

    @Test
    void theDefaultIconSentinelIsNotDescriptionText() {
        PlayerMock player = addPlayer();

        server.execute("create-home", player, "base", "d").assertSucceeded();
        server.execute("create-home", player, "camp", "default", "my", "spot").assertSucceeded();

        List<Home> homes = new HomesDao().getAll(player.getUniqueId());
        Home base = homes.stream().filter(h -> h.getName().equals("base")).findFirst().orElseThrow();
        Home camp = homes.stream().filter(h -> h.getName().equals("camp")).findFirst().orElseThrow();

        assertEquals(Material.WHITE_WOOL.name(), base.getMaterial());
        assertNull(base.getDescription());
        assertEquals(Material.WHITE_WOOL.name(), camp.getMaterial());
        assertEquals("my spot", camp.getDescription());
    }

    @Test
    void aDefaultIconThatNamesNoItemFallsBackToWhiteWool() {
        PlayerMock player = addPlayer();
        plugin.getConfig().set("defaultHomeItem", "water");

        server.execute("create-home", player, "base").assertSucceeded();

        assertEquals(Material.WHITE_WOOL.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void theConfiguredDefaultIconIsUsedWhenNoneIsGiven() {
        PlayerMock player = addPlayer();
        plugin.getConfig().set("defaultHomeItem", "chest");

        server.execute("create-home", player, "base").assertSucceeded();

        assertEquals(Material.CHEST.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }
}

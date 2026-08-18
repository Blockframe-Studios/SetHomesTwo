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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateHomeTest extends ServerTestBase {

    @Test
    void aHomeIsCreatedAtThePlayersLocation() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 12, 65, -8));

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("base", homes.get(0).getName());
        assertEquals(12.0, homes.get(0).getX());
    }

    @Test
    void aBareCommandCreatesTheDefaultHome() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 7, 65, 7));

        assertTrue(server.execute("create-home", player).hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "default").hasSucceeded());
        player.nextMessage();
        assertTrue(server.execute("create-home", player).hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "base", "default").hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "BASE").hasSucceeded());

        assertTrue(player.nextMessage().contains("You already have a home called"));
        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }

    @Test
    void aNonMaterialSecondArgumentBecomesTheDescription() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("create-home", player, "base", "my", "main", "base").hasSucceeded());

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(1, homes.size());
        assertEquals("my main base", homes.get(0).getDescription());
        assertEquals(Material.WHITE_WOOL.name(), homes.get(0).getMaterial());
    }

    @Test
    void aMaterialSecondArgumentStillSetsTheIcon() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("create-home", player, "base", "diamond_block", "my", "base").hasSucceeded());

        var homes = new HomesDao().getAll(player.getUniqueId());
        assertEquals(Material.DIAMOND_BLOCK.name(), homes.get(0).getMaterial());
        assertEquals("my base", homes.get(0).getDescription());
    }

    @Test
    void theChosenIconIsNamedInTheSuccessMessage() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("create-home", player, "base", "diamond_block").hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "base", "stone", "house").hasSucceeded());

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(Material.STONE.name(), home.getMaterial());
        assertEquals("house", home.getDescription());
    }

    @Test
    void aSuppliedMaterialIsStored() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("create-home", player, "base", "diamond").hasSucceeded());

        assertEquals(Material.DIAMOND.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void aBlacklistedDimensionIsRejected() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 0, 64, 0));
        HomeFixtures.blacklist(overworld.getName());

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "camp").hasSucceeded());

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
                assertTrue(server.execute("create-home", player, "camp").hasSucceeded()));

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
        assertTrue(server.execute("create-home", player, "base", "water", "front").hasSucceeded());

        Home home = new HomesDao().getAll(player.getUniqueId()).get(0);
        assertEquals(Material.WHITE_WOOL.name(), home.getMaterial());
        assertEquals("water front", home.getDescription());
    }

    @Test
    void theHomesMenuStillOpensAfterAHomeNamedAfterANonItem() {
        PlayerMock player = addPlayer();
        assertTrue(server.execute("create-home", player, "base", "water", "front").hasSucceeded());

        assertTrue(server.execute("homes", player).hasSucceeded());
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

        assertTrue(server.execute("create-home", player, "base", "d").hasSucceeded());
        assertTrue(server.execute("create-home", player, "camp", "default", "my", "spot").hasSucceeded());

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

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

        assertEquals(Material.WHITE_WOOL.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void theConfiguredDefaultIconIsUsedWhenNoneIsGiven() {
        PlayerMock player = addPlayer();
        plugin.getConfig().set("defaultHomeItem", "chest");

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

        assertEquals(Material.CHEST.name(), new HomesDao().getAll(player.getUniqueId()).get(0).getMaterial());
    }

    @Test
    void anEmptyHomeNameIsRejected() {
        PlayerMock player = addPlayer();

        // A double space makes Bukkit hand over an empty first argument. Saved
        // as-is it produces a home no command can name, so no command can
        // delete it either.
        server.dispatchCommand(player, "create-home  base");

        assertTrue(player.nextMessage().contains("must not be blank"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aHomeNameOverTheConfiguredLimitIsRejected() {
        PlayerMock player = addPlayer();
        plugin.getConfig().set("maxHomeNameLength", 8);

        assertTrue(server.execute("create-home", player, "waaaaaaaaaaaytoolong").hasSucceeded());

        assertTrue(player.nextMessage().contains("too long"));
        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void aNameWithinTheConfiguredLimitIsAccepted() {
        PlayerMock player = addPlayer();
        plugin.getConfig().set("maxHomeNameLength", 8);

        assertTrue(server.execute("create-home", player, "base").hasSucceeded());

        assertEquals(1, new HomesDao().getAll(player.getUniqueId()).size());
    }
}

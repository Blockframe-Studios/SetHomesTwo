package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.metrics.UsageCounters;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import com.samleighton.sethomestwo.tabcompleters.PlayerHomesTabCompleter;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.ClickType;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.gui.GuiSession;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHomeAdminCommandsTest extends ServerTestBase {

    @Test
    void deletingAnOfflinePlayersHomeWorks() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        UUID targetId = target.getUniqueId();
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        assertTrue(server.execute("delete-player-home", admin, "Steve", "base").hasSucceeded());

        assertTrue(new HomesDao(true).getAll(targetId).isEmpty());
    }

    @Test
    void movingAnOfflinePlayersHomeUsesTheAdminLocation() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        UUID targetId = target.getUniqueId();
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.move-player-home", true);
        admin.teleport(new Location(overworld, 250, 70, 250));

        assertTrue(server.execute("move-player-home", admin, "Steve", "base").hasSucceeded());

        assertEquals(250.0, new HomesDao(true).getAll(targetId).get(0).getX());
    }

    @Test
    void teleportingToAnOfflinePlayersHomeIsAccepted() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(target, "base", new Location(overworld, 30, 70, 30)));
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.go-player-home", true);
        admin.addAttachment(plugin, "sh2.teleport", true);
        admin.teleport(new Location(overworld, 0, 64, 0));
        plugin.getConfig().set("delay", 0);
        // TeleportSafetyUtil.prefetchChunks reaches an unimplemented MockBukkit call.
        plugin.getConfig().set("teleportSafety", false);

        assertTrue(server.execute("go-player-home", admin, "Steve", "base").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals(30.0, admin.getLocation().getX());
    }

    @Test
    void anUnknownPlayerIsReported() {
        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        assertTrue(server.execute("delete-player-home", admin, "Nobody", "base").hasSucceeded());

        assertTrue(admin.nextMessage().contains("No player by that name"));
    }

    @Test
    void eachCommandIsRefusedWithoutItsNodeAtTheCommandGate() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", false);

        assertTrue(server.execute("delete-player-home", admin, "Steve", "base").hasSucceeded());

        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theInCodeGuardAlsoRefuses() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", false);

        // Call the executor directly. Going through server.execute would be
        // refused by plugin.yml's permission before onCommand is reached.
        new DeletePlayerHome().onCommand(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"Steve", "base"});

        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theV1AliasesWork() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        assertTrue(server.execute("delhome-of", admin, "Steve", "base").hasSucceeded());

        assertTrue(new HomesDao(true).getAll(target.getUniqueId()).isEmpty());
    }

    @Test
    void theWrongNumberOfArgumentsShowsTheUsage() {
        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.move-player-home", true);

        assertTrue(server.execute("move-player-home", admin, "Steve").hasSucceeded());

        assertTrue(admin.nextMessage().contains("Incorrect number of arguments"));
        assertTrue(admin.nextMessage().contains("Usage: /move-player-home <player> <home>"));
    }

    @Test
    void theTabCompleterOffersOnlinePlayerNamesFirst() {
        addPlayer("Steve");
        PlayerMock admin = addPlayer("Admin");

        List<String> completions = new PlayerHomesTabCompleter().onTabComplete(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"St"});

        assertTrue(completions.contains("Steve"));
        assertFalse(completions.contains("Admin"));
    }

    @Test
    void theTabCompleterOffersTheTargetsHomeNamesSecond() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        HomeFixtures.persist(target, "mine");
        target.disconnect();

        PlayerMock admin = addPlayer("Admin");

        List<String> completions = new PlayerHomesTabCompleter().onTabComplete(
                admin,
                Objects.requireNonNull(plugin.getCommand("delete-player-home")),
                "delete-player-home",
                new String[]{"Steve", "b"});

        assertEquals(List.of("base"), completions);
    }

    @Test
    void theInCodeGuardAlsoRefusesTheOtherTwoCommands() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");
        double beforeX = new HomesDao(true).getAll(target.getUniqueId()).get(0).getX();

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.go-player-home", false);
        admin.addAttachment(plugin, "sh2.move-player-home", false);
        admin.teleport(new Location(overworld, 900, 70, 900));
        // So a regression that lets the teleport through fails on the assertion
        // below rather than aborting on an unimplemented mock call.
        plugin.getConfig().set("teleportSafety", false);

        new GoPlayerHome().onCommand(admin,
                Objects.requireNonNull(plugin.getCommand("go-player-home")),
                "go-player-home", new String[]{"Steve", "base"});
        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(900.0, admin.getLocation().getX());

        new MovePlayerHome().onCommand(admin,
                Objects.requireNonNull(plugin.getCommand("move-player-home")),
                "move-player-home", new String[]{"Steve", "base"});
        assertTrue(admin.nextMessage().contains("permission"));
        assertEquals(beforeX, new HomesDao(true).getAll(target.getUniqueId()).get(0).getX());
    }

    @Test
    void anUnknownHomeIsReportedByEachCommand() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);
        admin.addAttachment(plugin, "sh2.move-player-home", true);
        admin.addAttachment(plugin, "sh2.go-player-home", true);

        assertTrue(server.execute("delete-player-home", admin, "Steve", "nope").hasSucceeded());
        assertUnknownHomeNamed(admin, "nope");

        assertTrue(server.execute("move-player-home", admin, "Steve", "nope").hasSucceeded());
        assertUnknownHomeNamed(admin, "nope");

        assertTrue(server.execute("go-player-home", admin, "Steve", "nope").hasSucceeded());
        assertUnknownHomeNamed(admin, "nope");

        assertEquals(1, new HomesDao(true).getAll(target.getUniqueId()).size());
    }

    @Test
    void theSuccessMessageNamesTheOwnerAndHomeCanonically() {
        PlayerMock target = addPlayer("Steve");
        HomeFixtures.persist(target, "base");

        PlayerMock admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.delete-player-home", true);

        // Both names typed in the wrong case. The reply must echo the stored
        // spelling, not what was typed.
        assertTrue(server.execute("delete-player-home", admin, "sTeVe", "BaSe").hasSucceeded());

        String reply = admin.nextMessage();
        assertTrue(reply.contains("Steve"), reply);
        assertTrue(reply.contains("base"), reply);
    }

    /**
     * Every command must name the home it looked for. "That home no longer
     * exists" is the GUI's message, for a home that vanished mid-menu.
     */
    private static void assertUnknownHomeNamed(PlayerMock admin, String typed) {
        String message = admin.nextMessage();
        assertTrue(message.contains(typed), message);
        assertFalse(message.contains("%s"), message);
    }

    @Test
    void withoutTheBypassNodeAnAdminCannotReachAnotherPlayersBlacklistedHome() {
        TestPlayer owner = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(owner, "hideout", new Location(nether, 33, 70, 33)));
        HomeFixtures.blacklist("world_nether");
        owner.disconnect();

        TestPlayer admin = adminAt(new Location(overworld, 0, 70, 0));
        admin.addAttachment(plugin, "sh2.bypass-blacklist", false);

        assertTrue(server.execute("go-player-home", admin, "Steve", "hideout").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals("world", admin.getLocation().getWorld().getName());
        assertTrue(admin.nextMessage().contains("blacklisted"));
    }

    @Test
    void aBlacklistedTeleportCountsTheBlacklistedOutcome() {
        TestPlayer owner = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(owner, "hideout", new Location(nether, 33, 70, 33)));
        HomeFixtures.blacklist("world_nether");
        owner.disconnect();

        TestPlayer admin = adminAt(new Location(overworld, 0, 70, 0));
        admin.addAttachment(plugin, "sh2.bypass-blacklist", false);

        assertTrue(server.execute("go-player-home", admin, "Steve", "hideout").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals(1, plugin.getUsageCounters().snapshot(UsageCounters.Family.TELEPORT_OUTCOME).get(UsageCounters.OUTCOME_BLACKLISTED));
    }

    @Test
    void withTheBypassNodeAnAdminReachesAnotherPlayersBlacklistedHome() {
        TestPlayer owner = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(owner, "hideout", new Location(nether, 33, 70, 33)));
        HomeFixtures.blacklist("world_nether");
        owner.disconnect();

        TestPlayer admin = adminAt(new Location(overworld, 0, 70, 0));
        admin.addAttachment(plugin, "sh2.bypass-blacklist", true);

        assertTrue(server.execute("go-player-home", admin, "Steve", "hideout").hasSucceeded());
        server.getScheduler().performTicks(100L);

        assertEquals("world_nether", admin.getLocation().getWorld().getName());
        assertEquals(33.0, admin.getLocation().getX());
    }

    @Test
    void withoutTheBypassNodeClickingABlacklistedHomeInTheAdminViewDoesNotTeleport() {
        TestPlayer owner = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(owner, "hideout", new Location(nether, 33, 70, 33)));
        HomeFixtures.blacklist("world_nether");

        TestPlayer admin = adminAt(new Location(overworld, 0, 70, 0));
        admin.addAttachment(plugin, "sh2.get-player-homes", true);
        admin.addAttachment(plugin, "sh2.bypass-blacklist", false);

        assertTrue(server.execute("get-player-homes", admin, "Steve").hasSucceeded());
        clickFirstHome(admin);
        server.getScheduler().performTicks(100L);

        assertEquals("world", admin.getLocation().getWorld().getName());
    }

    @Test
    void withTheBypassNodeClickingABlacklistedHomeInTheAdminViewTeleports() {
        TestPlayer owner = addPlayer("Steve");
        HomeFixtures.persist(HomeFixtures.home(owner, "hideout", new Location(nether, 33, 70, 33)));
        HomeFixtures.blacklist("world_nether");

        TestPlayer admin = adminAt(new Location(overworld, 0, 70, 0));
        admin.addAttachment(plugin, "sh2.get-player-homes", true);
        admin.addAttachment(plugin, "sh2.bypass-blacklist", true);

        assertTrue(server.execute("get-player-homes", admin, "Steve").hasSucceeded());
        clickFirstHome(admin);
        server.getScheduler().performTicks(100L);

        assertEquals("world_nether", admin.getLocation().getWorld().getName());
    }

    /**
     * Left-click the first home of whatever list the player currently has open,
     * routed through the live GuiSession the command created.
     */
    private void clickFirstHome(TestPlayer player) {
        GuiSession session = plugin.getGuiSessionMap().get(player.getUniqueId());
        HomesGui gui = (HomesGui) session.getActiveScreen();

        gui.onClick(new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        ), session);
    }

    /**
     * An admin holding the two nodes every go-player-home test needs, standing
     * where the caller says, with the teleport machinery set to resolve inside
     * the ticks these tests advance.
     */
    private TestPlayer adminAt(Location where) {
        TestPlayer admin = addPlayer("Admin");
        admin.addAttachment(plugin, "sh2.go-player-home", true);
        admin.addAttachment(plugin, "sh2.teleport", true);
        admin.teleport(where);
        plugin.getConfig().set("delay", 0);
        plugin.getConfig().set("teleportSafety", false);
        return admin;
    }
}

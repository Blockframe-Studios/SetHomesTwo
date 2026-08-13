package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeActionsGuiTest extends ServerTestBase {

    private static final int SLOT_RENAME = 0;
    private static final int SLOT_MOVE = 1;
    private static final int SLOT_ICON = 2;
    private static final int SLOT_DELETE = 4;
    private static final int SLOT_BACK = 8;
    private static final int SLOT_CONFIRM = 2;
    private static final int SLOT_CANCEL = 6;

    private void click(HomeActionsGui gui, GuiSession session, PlayerMock player, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        gui.onClick(event, session);
    }

    private HomeActionsGui openSubmenu(PlayerMock player, Home home, GuiSession session) {
        HomeActionsGui gui = new HomeActionsGui(player, home);
        session.setActiveScreen(gui);
        gui.displayInventory(player);
        return gui;
    }

    @Test
    void actionOfIgnoresAnEmptySlot() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertNull(gui.actionOf(null));
        assertNull(gui.actionOf(new ItemStack(Material.AIR)));
    }

    @Test
    void actionOfReadsTheTagOffAButton() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);
        gui.displayInventory(player);

        assertEquals(HomeActionsGui.ACTION_RENAME, gui.actionOf(gui.getInventory().getItem(SLOT_RENAME)));
        assertEquals(HomeActionsGui.ACTION_BACK, gui.actionOf(gui.getInventory().getItem(SLOT_BACK)));
    }

    @Test
    void backReturnsToTheHomeList() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        HomesGui homesGui = new HomesGui(player);
        homesGui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        GuiSession session = new GuiSession(homesGui);

        HomeActionsGui gui = openSubmenu(player, home, session);
        click(gui, session, player, SLOT_BACK);

        assertInstanceOf(HomesGui.class, session.getActiveScreen());
    }

    @Test
    void deleteShowsTheConfirmationLayoutAndCancelRestoresIt() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        GuiSession session = new GuiSession(new HomesGui(player));

        HomeActionsGui gui = openSubmenu(player, home, session);
        click(gui, session, player, SLOT_DELETE);

        assertEquals(HomeActionsGui.ACTION_CONFIRM_DELETE, gui.actionOf(gui.getInventory().getItem(SLOT_CONFIRM)));
        assertEquals(HomeActionsGui.ACTION_CANCEL_DELETE, gui.actionOf(gui.getInventory().getItem(SLOT_CANCEL)));

        click(gui, session, player, SLOT_CANCEL);

        assertEquals(HomeActionsGui.ACTION_RENAME, gui.actionOf(gui.getInventory().getItem(SLOT_RENAME)));
        assertNotNull(gui.getInventory().getItem(SLOT_BACK));
    }

    @Test
    void confirmDeleteRemovesTheHome() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        HomesGui homesGui = new HomesGui(player);
        homesGui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        GuiSession session = new GuiSession(homesGui);

        HomeActionsGui gui = openSubmenu(player, home, session);
        click(gui, session, player, SLOT_DELETE);
        click(gui, session, player, SLOT_CONFIRM);

        assertTrue(new HomesDao().getAll(player.getUniqueId()).isEmpty());
    }

    @Test
    void moveWritesThePlayersCurrentLocation() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 10, 64, 10));
        Home home = HomeFixtures.persist(player, "base");

        HomesGui homesGui = new HomesGui(player);
        homesGui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        GuiSession session = new GuiSession(homesGui);

        // Teleport before opening the submenu: PlayerMock.teleport() closes any open
        // inventory view, leaving one whose convertSlot() MockBukkit doesn't implement.
        player.teleport(new Location(overworld, 200, 70, -150));
        HomeActionsGui gui = openSubmenu(player, home, session);
        click(gui, session, player, SLOT_MOVE);

        Home reloaded = new HomesDao().getById(player.getUniqueId(), home.getId());
        assertNotNull(reloaded);
        assertEquals(200.0, reloaded.getX());
        assertEquals(70.0, reloaded.getY());
        assertEquals(-150.0, reloaded.getZ());
    }

    @Test
    void moveIntoABlacklistedDimensionIsRefusedAndWritesNothing() {
        PlayerMock player = addPlayer();
        player.teleport(new Location(overworld, 10, 64, 10));
        Home home = HomeFixtures.persist(player, "base");
        HomeFixtures.blacklist(overworld.getName());

        GuiSession session = new GuiSession(new HomesGui(player));

        // See the comment in moveWritesThePlayersCurrentLocation: teleport before
        // opening the submenu so MockBukkit doesn't close the GUI's inventory view.
        player.teleport(new Location(overworld, 500, 70, 500));
        HomeActionsGui gui = openSubmenu(player, home, session);
        click(gui, session, player, SLOT_MOVE);

        Home reloaded = new HomesDao().getById(player.getUniqueId(), home.getId());
        assertNotNull(reloaded);
        assertEquals(10.0, reloaded.getX());
        assertTrue(player.nextMessage().contains("You cannot move a home into this dimension"));
    }

    @Test
    void setIconRefusesAnEmptyHand() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        GuiSession session = new GuiSession(new HomesGui(player));

        HomeActionsGui gui = openSubmenu(player, home, session);
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        click(gui, session, player, SLOT_ICON);

        assertTrue(player.nextMessage().contains("Hold the item you want to use as the icon"));
        assertEquals(Material.WHITE_WOOL.name(), new HomesDao().getById(player.getUniqueId(), home.getId()).getMaterial());
    }

    @Test
    void setIconTakesTheHeldItem() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        HomesGui homesGui = new HomesGui(player);
        homesGui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        GuiSession session = new GuiSession(homesGui);

        HomeActionsGui gui = openSubmenu(player, home, session);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));
        click(gui, session, player, SLOT_ICON);

        assertEquals(Material.DIAMOND.name(), new HomesDao().getById(player.getUniqueId(), home.getId()).getMaterial());
    }

    @Test
    void applyRenameRejectsABlankName() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertEquals(HomeActionsGui.RenameOutcome.EMPTY, gui.applyRename(player, home, "   ", 32));
        assertEquals("base", new HomesDao().getById(player.getUniqueId(), home.getId()).getName());
    }

    @Test
    void applyRenameRejectsAnOverLongName() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertEquals(HomeActionsGui.RenameOutcome.TOO_LONG, gui.applyRename(player, home, "a".repeat(33), 32));
        assertTrue(player.nextMessage().contains("too long"));
    }

    @Test
    void applyRenameRejectsAnotherHomesName() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeFixtures.persist(player, "camp");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertEquals(HomeActionsGui.RenameOutcome.DUPLICATE, gui.applyRename(player, home, "camp", 32));
        assertEquals("base", new HomesDao().getById(player.getUniqueId(), home.getId()).getName());
    }

    @Test
    void applyRenameAllowsTheHomesOwnName() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertEquals(HomeActionsGui.RenameOutcome.RENAMED, gui.applyRename(player, home, "base", 32));
    }

    @Test
    void applyRenamePersistsTheNewName() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");
        HomeActionsGui gui = new HomeActionsGui(player, home);

        assertEquals(HomeActionsGui.RenameOutcome.RENAMED, gui.applyRename(player, home, "  camp  ", 32));
        assertEquals("camp", new HomesDao().getById(player.getUniqueId(), home.getId()).getName());
    }
}

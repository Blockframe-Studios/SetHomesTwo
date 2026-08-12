package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomesGuiClickTest extends ServerTestBase {

    private void click(HomesGui gui, GuiSession session, PlayerMock player, int slot, ClickType type) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                type,
                InventoryAction.PICKUP_ALL
        );
        gui.onClick(event, session);
    }

    private HomesGui openOwnList(PlayerMock player) {
        HomesGui gui = new HomesGui(player);
        gui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        gui.displayInventory(player);
        return gui;
    }

    @Test
    void rightClickOnOwnListOpensTheManagementSubmenu() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        HomesGui gui = openOwnList(player);
        GuiSession session = new GuiSession(gui);

        click(gui, session, player, 0, ClickType.RIGHT);

        assertInstanceOf(HomeActionsGui.class, session.getActiveScreen());
    }

    @Test
    void rightClickWithoutManagePermissionDoesNothing() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.manage-homes", false);
        HomeFixtures.persist(player, "base");

        HomesGui gui = openOwnList(player);
        GuiSession session = new GuiSession(gui);

        click(gui, session, player, 0, ClickType.RIGHT);

        assertNull(session.getActiveScreen());
    }

    @Test
    void rightClickOnAHomeDeletedUnderneathThePlayerReportsItIsGone() {
        PlayerMock player = addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        HomesGui gui = openOwnList(player);
        GuiSession session = new GuiSession(gui);

        // Remove the row after the menu was drawn.
        new HomesDao().delete(home);

        click(gui, session, player, 0, ClickType.RIGHT);

        assertTrue(player.nextMessage().contains("That home no longer exists."));
        assertNull(session.getActiveScreen());
    }

    @Test
    void rightClickOnTheAdminListDoesNotOpenTheSubmenu() {
        PlayerMock admin = addTestPlayer("admin");

        // The clicked home must belong to the clicker, or the ownership-scoped
        // getById lookup inside onClick would return null and take the "home no
        // longer exists" branch instead - which also never opens the submenu,
        // rescuing this test for the wrong reason. Give admin a home of their
        // own so that lookup succeeds, and isOwnList is the only thing left
        // standing between the click and the management submenu.
        HomeFixtures.persist(admin, "base");

        // Falling through to teleport behaviour actually invokes Home.teleport(),
        // which with teleport safety on would prefetch chunks via a WorldMock API
        // MockBukkit does not implement. That machinery is not what this test is
        // about, so it is turned off to let the routing decision run to completion.
        plugin.getConfig().set("teleportSafety", false);

        // The admin view is the two-argument constructor, which sets isOwnList
        // false, even though it is populated with admin's own home here so the
        // ownership lookup succeeds and only isOwnList is under test.
        HomesGui adminGui = new HomesGui(admin, "Homes of " + admin.getName());
        adminGui.setHomes(new HomesDao(true).getAll(admin.getUniqueId()));
        adminGui.displayInventory(admin);

        GuiSession session = new GuiSession(new HomesGui(admin));
        session.setActiveScreen(adminGui);

        click(adminGui, session, admin, 0, ClickType.RIGHT);

        // Falls through to teleport behaviour rather than management, even though
        // the home lookup would have succeeded: isOwnList is what stops it.
        assertInstanceOf(HomesGui.class, session.getActiveScreen());
        assertFalse(session.getActiveScreen() instanceof HomeActionsGui);
    }

    @Test
    void anEmptyHomeListClosesTheMenuAndExplainsWhy() {
        PlayerMock player = addPlayer();

        HomesGui gui = new HomesGui(player);
        gui.setHomes(new HomesDao().getAll(player.getUniqueId()));
        gui.displayInventory(player);

        assertTrue(player.nextMessage().contains("You have not created any homes yet."));
    }
}

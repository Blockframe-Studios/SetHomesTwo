package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.metrics.UsageCounters;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        // Admin needs their own home, or the ownership-scoped lookup fails and
        // takes the "gone" branch instead, passing this test for the wrong reason.
        HomeFixtures.persist(admin, "base");

        // Turned off because falling through invokes Home.teleport, which would
        // otherwise prefetch chunks via a WorldMock API MockBukkit doesn't implement.
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
    void leftClickCountsAGuiTeleport() {
        TestPlayer player = addTestPlayer("traveller");
        HomeFixtures.persist(player, "base");
        plugin.getConfig().set("teleportSafety", false);

        HomesGui gui = openOwnList(player);
        click(gui, new GuiSession(gui), player, 0, ClickType.LEFT);

        UsageCounters counters = plugin.getUsageCounters();
        assertEquals(1, counters.snapshot(UsageCounters.Family.GUI_ACTION).get(UsageCounters.GUI_TELEPORT));
        assertEquals(1, counters.snapshot(UsageCounters.Family.TELEPORT_SOURCE).get(UsageCounters.SOURCE_GUI));
    }

    @Test
    void rightClickIntoManagementCountsNoTeleport() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        HomesGui gui = openOwnList(player);
        click(gui, new GuiSession(gui), player, 0, ClickType.RIGHT);

        assertTrue(plugin.getUsageCounters().snapshot(UsageCounters.Family.GUI_ACTION).isEmpty());
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

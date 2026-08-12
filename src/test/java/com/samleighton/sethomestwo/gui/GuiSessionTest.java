package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSessionTest extends ServerTestBase {

    /**
     * Minimal GuiScreen that only records whether it was routed a click.
     */
    private static final class RecordingScreen implements GuiScreen {

        private final Inventory inventory;
        private int clicks = 0;

        private RecordingScreen(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void onClick(InventoryClickEvent event, GuiSession session) {
            clicks++;
        }
    }

    private InventoryClickEvent clickOn(PlayerMock player, Inventory inventory, int slot) {
        InventoryView view = player.openInventory(inventory);
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    @Test
    void clickIsIgnoredWhenNoScreenIsActive() {
        PlayerMock player = server.addPlayer();
        GuiSession session = new GuiSession(new HomesGui(player));

        InventoryClickEvent event = clickOn(player, Bukkit.createInventory(player, 9, "other"), 0);
        session.handleClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void clickInAForeignInventoryIsNeitherCancelledNorRouted() {
        PlayerMock player = server.addPlayer();
        GuiSession session = new GuiSession(new HomesGui(player));

        RecordingScreen screen = new RecordingScreen(Bukkit.createInventory(player, 9, "active"));
        session.setActiveScreen(screen);

        InventoryClickEvent event = clickOn(player, Bukkit.createInventory(player, 9, "foreign"), 0);
        session.handleClick(event);

        assertFalse(event.isCancelled());
        assertEquals(0, screen.clicks);
    }

    @Test
    void clickInTheActiveScreenIsCancelledAndRouted() {
        PlayerMock player = server.addPlayer();
        GuiSession session = new GuiSession(new HomesGui(player));

        Inventory inventory = Bukkit.createInventory(player, 9, "active");
        RecordingScreen screen = new RecordingScreen(inventory);
        session.setActiveScreen(screen);

        InventoryClickEvent event = clickOn(player, inventory, 0);
        session.handleClick(event);

        assertTrue(event.isCancelled());
        assertEquals(1, screen.clicks);
    }

    @Test
    void openHomeListMakesTheHomeListActive() {
        PlayerMock player = server.addPlayer();
        HomeFixtures.persist(player, "base");

        HomesGui homesGui = new HomesGui(player);
        homesGui.setHomes(new com.samleighton.sethomestwo.dao.HomesDao().getAll(player.getUniqueId()));

        GuiSession session = new GuiSession(homesGui);
        session.openHomeList(player);

        assertSame(homesGui, session.getActiveScreen());
    }

    @Test
    void openHomeActionsMakesTheSubmenuActive() {
        PlayerMock player = server.addPlayer();
        Home home = HomeFixtures.persist(player, "base");

        GuiSession session = new GuiSession(new HomesGui(player));
        session.openHomeActions(player, home);

        assertInstanceOf(HomeActionsGui.class, session.getActiveScreen());
    }
}

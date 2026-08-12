package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.datatypes.PersistentHome;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HomesGuiPaginationTest extends ServerTestBase {

    private static final int NEXT_PAGE_SLOT = 53;
    private static final int PREV_PAGE_SLOT = 45;

    private List<Home> homes(PlayerMock player, int count) {
        List<Home> homes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            homes.add(HomeFixtures.home(player, "home-" + i));
        }
        return homes;
    }

    /**
     * Count the items carrying the persistent home tag, so page buttons are not
     * mistaken for homes.
     */
    private int homeItemCount(Inventory inventory) {
        NamespacedKey key = new NamespacedKey(SetHomesTwo.instance(), "home");
        int count = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getItemMeta() == null) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(key, new PersistentHome())) count++;
        }

        return count;
    }

    private void clickSlot(HomesGui gui, GuiSession session, PlayerMock player, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        gui.onClick(event, session);
    }

    @Test
    void fortyFiveHomesFitOnASinglePage() {
        PlayerMock player = server.addPlayer();
        HomesGui gui = new HomesGui(player);
        gui.setHomes(homes(player, 45));
        gui.displayInventory(player);

        assertEquals(45, homeItemCount(gui.getInventory()));
        assertNull(gui.getInventory().getItem(NEXT_PAGE_SLOT), "45 homes must not produce a second page");
    }

    @Test
    void fortySixHomesSplitFortyFiveThenOne() {
        PlayerMock player = server.addPlayer();
        HomesGui gui = new HomesGui(player);
        GuiSession session = new GuiSession(gui);
        gui.setHomes(homes(player, 46));
        gui.displayInventory(player);

        assertEquals(45, homeItemCount(gui.getInventory()));
        assertNotNull(gui.getInventory().getItem(NEXT_PAGE_SLOT));

        clickSlot(gui, session, player, NEXT_PAGE_SLOT);

        assertEquals(1, homeItemCount(gui.getInventory()));
        assertNotNull(gui.getInventory().getItem(PREV_PAGE_SLOT));
    }

    @Test
    void ninetyHomesFillTwoPagesWithNoneHidden() {
        PlayerMock player = server.addPlayer();
        HomesGui gui = new HomesGui(player);
        GuiSession session = new GuiSession(gui);
        gui.setHomes(homes(player, 90));
        gui.displayInventory(player);

        assertEquals(45, homeItemCount(gui.getInventory()));

        clickSlot(gui, session, player, NEXT_PAGE_SLOT);

        // The previous-page button occupies slot 45, so a page that packed 46
        // homes would silently lose the one underneath it.
        assertEquals(45, homeItemCount(gui.getInventory()));
        assertEquals(Material.RED_STAINED_GLASS_PANE, gui.getInventory().getItem(PREV_PAGE_SLOT).getType());
    }
}

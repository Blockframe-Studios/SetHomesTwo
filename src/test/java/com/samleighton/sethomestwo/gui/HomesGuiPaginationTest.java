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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Collect the display names of every home-tagged item in the inventory, so
     * a test can assert on which homes are present rather than merely how many -
     * a count alone cannot distinguish "45 correct homes" from "44 correct homes
     * plus one duplicate."
     */
    private Set<String> homeNames(Inventory inventory) {
        NamespacedKey key = new NamespacedKey(SetHomesTwo.instance(), "home");
        Set<String> names = new HashSet<>();

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getItemMeta() == null) continue;
            if (!item.getItemMeta().getPersistentDataContainer().has(key, new PersistentHome())) continue;
            names.add(item.getItemMeta().getDisplayName());
        }

        return names;
    }

    /**
     * The expected set of fixture names "home-{start}" .. "home-{endExclusive - 1}".
     */
    private Set<String> expectedNames(int start, int endExclusive) {
        Set<String> names = new HashSet<>();
        for (int i = start; i < endExclusive; i++) {
            names.add("home-" + i);
        }
        return names;
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

        // Identity, not just count: page 0 must be exactly home-0..home-44, so a
        // regression that dropped one of these and duplicated another would not
        // slip past a count-only check.
        assertEquals(expectedNames(0, 45), homeNames(gui.getInventory()));
        assertNotNull(gui.getInventory().getItem(NEXT_PAGE_SLOT));

        clickSlot(gui, session, player, NEXT_PAGE_SLOT);

        // The 46th home (home-45) must be the one that rolled over, not some
        // other home standing in for it.
        assertEquals(expectedNames(45, 46), homeNames(gui.getInventory()));
        assertNotNull(gui.getInventory().getItem(PREV_PAGE_SLOT));
    }

    @Test
    void ninetyHomesFillTwoPagesWithNoneHidden() {
        PlayerMock player = server.addPlayer();
        HomesGui gui = new HomesGui(player);
        GuiSession session = new GuiSession(gui);
        gui.setHomes(homes(player, 90));
        gui.displayInventory(player);

        // Identity, not just count: a count of 45 is also what you would see if
        // one home were dropped and another duplicated in its place. Pin exactly
        // which homes are on this page.
        Set<String> page0Names = homeNames(gui.getInventory());
        assertEquals(expectedNames(0, 45), page0Names);

        clickSlot(gui, session, player, NEXT_PAGE_SLOT);

        // The previous-page button occupies slot 45, so a page that packed 46
        // homes would silently lose the one underneath it.
        Set<String> page1Names = homeNames(gui.getInventory());
        assertEquals(expectedNames(45, 90), page1Names);
        assertEquals(Material.RED_STAINED_GLASS_PANE, gui.getInventory().getItem(PREV_PAGE_SLOT).getType());

        // The union across both pages must be all 90 distinct homes: nothing
        // missing, nothing duplicated across the page boundary.
        Set<String> union = new HashSet<>(page0Names);
        union.addAll(page1Names);
        assertEquals(expectedNames(0, 90), union);
    }
}

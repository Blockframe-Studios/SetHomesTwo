package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.datatypes.PersistentString;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.items.HomeItem;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RightClickHomeItemTest extends ServerTestBase {

    private void interact(TestPlayer player, Action action, ItemStack item) {
        server.getPluginManager().callEvent(new PlayerInteractEvent(player, action, item, null, null));
    }

    private GuiSession sessionFor(TestPlayer player) {
        return plugin.getGuiSessionMap().get(player.getUniqueId());
    }

    /**
     * Build a drag event on the player's currently open inventory, matching
     * GuiSessionTest's dragOn helper.
     */
    private InventoryDragEvent dragOn(TestPlayer player) {
        Map<Integer, ItemStack> newItems = new HashMap<>();
        newItems.put(0, new ItemStack(Material.DIAMOND, 1));

        return new InventoryDragEvent(
                player.getOpenInventory(), null, new ItemStack(Material.DIAMOND, 1), false, newItems);
    }

    /**
     * Carries both home-item tags but not the configured material, isolating
     * the material check from the rest of the guard.
     */
    private ItemStack taggedItem(Material material, TestPlayer owner) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SetHomesTwo.instance(), "belongs-to"), new PersistentString(), owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SetHomesTwo.instance(), "list-id"), new PersistentString(), java.util.UUID.randomUUID().toString());
        item.setItemMeta(meta);
        return item;
    }

    @Test
    void rightClickingAHomeItemOpensTheHomesList() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        interact(player, Action.RIGHT_CLICK_AIR, new HomeItem(player));

        GuiSession session = sessionFor(player);
        assertNotNull(session);
        assertNotNull(session.getActiveScreen());
    }

    @Test
    void leftClickingAHomeItemIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        interact(player, Action.LEFT_CLICK_AIR, new HomeItem(player));

        assertNull(sessionFor(player).getActiveScreen());
    }

    @Test
    void anUntaggedCompassIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        interact(player, Action.RIGHT_CLICK_AIR, new ItemStack(Material.COMPASS, 1));

        assertNull(sessionFor(player).getActiveScreen());
        // Proves the click was dropped silently by the tag-presence guard,
        // not merely stopped by a different, noisier guard further down
        // (e.g. the ownership check, which also leaves activeScreen null).
        player.assertNoMoreSaid();
    }

    @Test
    void aDifferentMaterialIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        // Tagged with the player's own uuid so the tag-presence, permission,
        // and ownership guards all pass - the material check is the only
        // thing left that can reject this click.
        interact(player, Action.RIGHT_CLICK_AIR, taggedItem(Material.DIAMOND, player));

        assertNull(sessionFor(player).getActiveScreen());
        player.assertNoMoreSaid();
    }

    @Test
    void withoutTeleportPermissionTheItemIsRefused() {
        TestPlayer player = addTestPlayer("owner");
        player.addAttachment(plugin, "sh2.teleport", false);
        HomeFixtures.persist(player, "base");

        interact(player, Action.RIGHT_CLICK_AIR, new HomeItem(player));

        assertTrue(player.nextMessage().contains("do not have permission"));
        assertNull(sessionFor(player).getActiveScreen());
    }

    @Test
    void anotherPlayersHomeItemIsRefused() {
        TestPlayer owner = addTestPlayer("owner");
        TestPlayer thief = addTestPlayer("thief");
        HomeFixtures.persist(owner, "base");

        // The item is tagged with the owner's uuid; the thief is holding it.
        interact(thief, Action.RIGHT_CLICK_AIR, new HomeItem(owner));

        assertTrue(thief.nextMessage().contains("does not belong to you"));
        assertNull(sessionFor(thief).getActiveScreen());
    }

    @Test
    void anInventoryClickIsRoutedToTheSessionAndCancelled() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        // Turned off because the click falls through to Home.teleport, which
        // prefetches chunks via a WorldMock API MockBukkit doesn't implement.
        plugin.getConfig().set("teleportSafety", false);

        interact(player, Action.RIGHT_CLICK_AIR, new HomeItem(player));
        GuiSession session = sessionFor(player);
        assertTrue(session.getActiveScreen() instanceof HomesGui);

        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void anInventoryClickFromAPlayerWithNoSessionIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        plugin.getGuiSessionMap().clear();

        player.openInventory(org.bukkit.Bukkit.createInventory(player, 9, "unrelated"));
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void anInventoryDragIsRoutedToTheSessionAndCancelled() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        interact(player, Action.RIGHT_CLICK_AIR, new HomeItem(player));
        GuiSession session = sessionFor(player);
        assertTrue(session.getActiveScreen() instanceof HomesGui);

        InventoryDragEvent event = dragOn(player);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void anInventoryDragFromAPlayerWithNoSessionIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        plugin.getGuiSessionMap().clear();

        player.openInventory(org.bukkit.Bukkit.createInventory(player, 9, "unrelated"));
        InventoryDragEvent event = dragOn(player);
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }
}

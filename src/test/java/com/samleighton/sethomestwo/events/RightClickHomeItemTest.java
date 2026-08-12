package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.items.HomeItem;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void aDifferentMaterialIsIgnored() {
        TestPlayer player = addTestPlayer("owner");
        HomeFixtures.persist(player, "base");

        interact(player, Action.RIGHT_CLICK_AIR, new ItemStack(Material.DIAMOND, 1));

        assertNull(sessionFor(player).getActiveScreen());
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

        // The left click below on slot 0 falls through HomesGui.onClick into
        // Home.teleport, which - same as HomesGuiClickTest's admin-view test -
        // would otherwise prefetch chunks via a WorldMock API MockBukkit does
        // not implement. That teleport machinery is not what this test is
        // about; it only cares that the click reached the session and got
        // cancelled, so teleport safety is turned off to let it run to
        // completion instead of aborting as skipped.
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
}

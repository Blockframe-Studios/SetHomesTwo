package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.datatypes.PersistentHome;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HomesGui implements GuiScreen {
    private final Inventory inv;
    private final int inventoryWidth = 9;
    private final int inventoryHeight = 6;
    private final int inventorySize = inventoryWidth * inventoryHeight;

    private final Map<Integer, List<Home>> pagesMap = new HashMap<>();
    private int currentPage = 0;
    private int maxPages = 1;

    private final String defaultBackPageMaterial = "red_stained_glass_pane";
    private final String defaultNextPageMaterial = "green_stained_glass_pane";

    // True when this screen is the viewer's own home list; false for the
    // admin view of another player's homes (GetPlayerHomes), where the
    // right-click management submenu must not be offered.
    private final boolean isOwnList;

    public HomesGui(Player player) {
        String title = ConfigUtil.getConfig().getString("inventoryTitle", "Your homes");

        // Create a 6x9 double chest inventory
        inv = Bukkit.createInventory(player, inventorySize, title);
        this.isOwnList = true;
    }

    public HomesGui(Player player, String title) {
        inv = Bukkit.createInventory(player, inventorySize, title);
        this.isOwnList = false;
    }

    // Ingest players homes into a hash map of home lists for pagination
    public void setHomes(List<Home> homes) {
        // Clear any preexisting homes
        pagesMap.clear();

        // Determine slots available
        int inventorySlotsAvailable = inventorySize - inventoryWidth;

        this.currentPage = 0;
        this.maxPages = (homes.size() / inventorySlotsAvailable) + 1;

        for (int i = 0; i < this.maxPages; i++) {
            pagesMap.put(i, new ArrayList<>());
        }

        int pageToBuild = 0;
        int slotIndex = 0;
        for (Home home : homes) {
            // Add home to current page being built
            pagesMap.get(pageToBuild).add(home);

            // Increment page and reset slot index if
            // index has reached last slot available.
            if (slotIndex == inventorySlotsAvailable) {
                slotIndex = 0;
                pageToBuild++;
                continue;
            }

            // Increment slot index
            slotIndex++;
        }
    }

    /**
     * Draws the blocks for each home in the inventory.
     */
    public void displayInventory(Player player) {
        inv.clear();

        // Get homes for current page
        List<Home> homesForDisplay = pagesMap.get(this.currentPage);

        if (homesForDisplay == null || homesForDisplay.isEmpty()) {
            player.closeInventory();
            String noHomesError = ConfigUtil.getConfig().getString("noHomes", UserError.NO_HOMES.getValue());
            ChatUtils.sendError(player, noHomesError);
            return;
        }

        // Draw home items in inventory
        for (Home home : homesForDisplay) {
            inv.addItem(createGuiItem(Material.matchMaterial(home.getMaterial()), home));
        }

        // Open inventory without page items
        if (!(maxPages > 1)) {
            player.openInventory(inv);
            return;
        }

        // Build previous page item
        Material backPageMaterial = Material.matchMaterial(ConfigUtil.getConfig().getString("previousPageItem", defaultBackPageMaterial));
        ItemStack prevPageItem = new ItemStack(Objects.requireNonNull(backPageMaterial), 1);
        ItemMeta prevPageItemMeta = prevPageItem.getItemMeta();
        Objects.requireNonNull(prevPageItemMeta).setDisplayName(ChatColor.DARK_RED + "Previous Page");
        prevPageItem.setItemMeta(prevPageItemMeta);

        // Build next page item
        Material nextPageMaterial = Material.matchMaterial(ConfigUtil.getConfig().getString("nextPageItem", defaultNextPageMaterial));
        ItemStack nextPageItem = new ItemStack(Objects.requireNonNull(nextPageMaterial), 1);
        ItemMeta nextPageItemMeta = nextPageItem.getItemMeta();
        Objects.requireNonNull(nextPageItemMeta).setDisplayName(ChatColor.DARK_GREEN + "Next Page");
        nextPageItem.setItemMeta(nextPageItemMeta);

        // Set prev page to bottom left of inventory.
        if (currentPage != 0) {
            inv.setItem(inventorySize - inventoryWidth, prevPageItem);
        }

        // Set next page to bottom right of inventory
        if (currentPage != maxPages - 1) {
            inv.setItem(inventorySize - 1, nextPageItem);
        }

        player.openInventory(inv);
    }

    /**
     * Create a new item to be placed in the inventory.
     *
     * @param mat,  The material of the item
     * @param home, The home designated for this item
     * @return ItemStack
     */
    protected ItemStack createGuiItem(final Material mat, @NotNull Home home) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();

        // Setup item lore and display name
        Objects.requireNonNull(meta).setDisplayName(home.getName());
        if (home.getDescription() != null)
            Objects.requireNonNull(meta).setLore(Collections.singletonList(home.getDescription()));

        // Persist the home to the item
        NamespacedKey homeKey = new NamespacedKey(SetHomesTwo.getPlugin(SetHomesTwo.class), "home");
        meta.getPersistentDataContainer().set(homeKey, new PersistentHome(), home);

        // Apply item meta
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    @Override
    public void onClick(InventoryClickEvent event, GuiSession session) {
        ItemStack clickedItem = event.getCurrentItem();
        NamespacedKey homeKey = new NamespacedKey(SetHomesTwo.getPlugin(SetHomesTwo.class), "home");

        // Guard for user actually clicking item.
        if (clickedItem == null || clickedItem.getType().isAir() || clickedItem.getItemMeta() == null) return;

        Player player = (Player) event.getWhoClicked();

        // Guard to check if item is actually home destination
        if (!clickedItem.getItemMeta().getPersistentDataContainer().has(homeKey, new PersistentHome())) {
            Material backPageMaterial = Material.matchMaterial(ConfigUtil.getConfig().getString("previousPageItem", defaultBackPageMaterial));
            Material nextPageMaterial = Material.matchMaterial(ConfigUtil.getConfig().getString("nextPageItem", defaultNextPageMaterial));

            // Guard to check if item is pagination material
            if (!(clickedItem.getType().equals(backPageMaterial) || clickedItem.getType().equals(nextPageMaterial)))
                return;

            // Move to prev page
            if (clickedItem.getType().equals(backPageMaterial)) currentPage--;

            // Move to next page
            if (clickedItem.getType().equals(nextPageMaterial)) currentPage++;

            // Display new inv state to player
            this.displayInventory(player);
            return;
        }

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();

        // Attempt to get the home from the clicked item
        Home home = clickedItemMeta.getPersistentDataContainer().get(homeKey, new PersistentHome());

        // The home object was not retrievable via item meta
        if (home == null) return;

        // Right-click opens management, left-click teleports. Management is only
        // offered on the viewer's own list; the admin view of another player's
        // homes falls through to teleport behaviour on any click.
        if (event.isRightClick() && isOwnList) {
            if (!player.hasPermission("sh2.manage-homes")) return;

            HomesDao homesDao = new HomesDao();
            Home fresh = home.getId() == null ? null : homesDao.getById(player.getUniqueId(), home.getId());

            if (fresh == null) {
                player.closeInventory();
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString("homeNoLongerExists", UserError.HOME_NO_LONGER_EXISTS.getValue()));
                return;
            }

            session.openHomeActions(player, fresh);
            return;
        }

        player.closeInventory();

        // Teleport player to home
        home.teleport(player);
    }
}

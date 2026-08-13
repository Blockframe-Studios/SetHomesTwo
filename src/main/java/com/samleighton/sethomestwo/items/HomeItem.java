package com.samleighton.sethomestwo.items;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.datatypes.PersistentString;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serializable;
import java.util.*;


public class HomeItem extends ItemStack implements Serializable {

    private UUID homeItemUUID;
    private UUID playerUUID;

    public HomeItem(Player player) {
        // Instantiate the base item
        super(Objects.requireNonNull(Material.matchMaterial(ConfigUtil.getConfig().getString("openHomeItem", Material.COMPASS.name()))), 1);

        // Setup item UUID and player
        setHomeItemUUID(UUID.randomUUID());
        setPlayerUUID(player.getUniqueId());

        // Setup Item meta
        ItemMeta initItemMeta = this.getItemMeta();
        String itemDisplayName = ConfigUtil.getConfig().getString("homeItemName", "Home's of %s");
        Objects.requireNonNull(initItemMeta).setDisplayName(String.format(itemDisplayName, player.getDisplayName()));

        // Setup item lore
        String itemLore = ConfigUtil.getConfig().getString("homeItemLore", "Right click this item to open your home's list.");
        List<String> baseLore = new ArrayList<>(Collections.singletonList(itemLore));
        Objects.requireNonNull(initItemMeta).setLore(baseLore);

        // Setup persistent data
        NamespacedKey playerKey = new NamespacedKey(SetHomesTwo.instance(), "belongs-to");
        initItemMeta.getPersistentDataContainer().set(playerKey, new PersistentString(), getPlayerUUID().toString());

        NamespacedKey homeKey = new NamespacedKey(SetHomesTwo.instance(), "list-id");
        initItemMeta.getPersistentDataContainer().set(homeKey, new PersistentString(), getHomeUUID().toString());

        // Apply item meta
        this.setItemMeta(initItemMeta);
    }

    protected UUID getHomeUUID() {
        return homeItemUUID;
    }

    public void setHomeItemUUID(UUID homeItemUUID) {
        this.homeItemUUID = homeItemUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
}

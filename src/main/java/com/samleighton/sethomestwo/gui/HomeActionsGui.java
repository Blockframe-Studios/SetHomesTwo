package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.datatypes.PersistentString;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.HomeNameValidator;
import com.samleighton.sethomestwo.utils.ServerUtil;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * The management submenu for a single home. Addressed by home id so a rename
 * cannot strand it.
 */
public class HomeActionsGui implements GuiScreen {

    public static final String ACTION_KEY_NAME = "gui-action";

    public static final String ACTION_RENAME = "rename";
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_ICON = "icon";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_CONFIRM_DELETE = "confirm-delete";
    public static final String ACTION_CANCEL_DELETE = "cancel-delete";
    public static final String ACTION_BACK = "back";

    private static final int SLOT_RENAME = 0;
    private static final int SLOT_MOVE = 1;
    private static final int SLOT_ICON = 2;
    private static final int SLOT_DELETE = 4;
    private static final int SLOT_BACK = 8;

    private static final int SLOT_CONFIRM = 2;
    private static final int SLOT_CANCEL = 6;

    private final Inventory inv;
    private final int homeId;
    private final String homeName;
    private boolean confirmingDelete = false;

    public HomeActionsGui(Player player, Home home) {
        this.homeId = home.getId();
        this.homeName = home.getName();

        String titleTemplate = ConfigUtil.getConfig().getString("manageHomeTitle", "Manage: %s");
        this.inv = Bukkit.createInventory(player, 9, String.format(titleTemplate, home.getName()));
    }

    public int getHomeId() {
        return homeId;
    }

    public String getHomeName() {
        return homeName;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private static NamespacedKey actionKey() {
        return new NamespacedKey(SetHomesTwo.getPlugin(SetHomesTwo.class), ACTION_KEY_NAME);
    }

    /**
     * Build a button carrying its action as persistent data.
     *
     * @param configKey      Config key holding the material name
     * @param defaultMat     Material used when the config value is absent or invalid
     * @param displayNameKey Config key holding the button label
     * @param defaultLabel   Label used when the config value is absent
     * @param action         The action constant to tag onto the item
     * @return The finished button
     */
    private ItemStack button(String configKey, Material defaultMat, String displayNameKey, String defaultLabel, String action) {
        Material material = Material.matchMaterial(ConfigUtil.getConfig().getString(configKey, defaultMat.name()));
        if (material == null || !material.isItem()) material = defaultMat;

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getConfig().getString(displayNameKey, defaultLabel)));
        meta.getPersistentDataContainer().set(actionKey(), new PersistentString(), action);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Draw the menu in its current mode and show it to the player.
     *
     * @param player The viewing player
     */
    public void displayInventory(Player player) {
        inv.clear();

        if (confirmingDelete) {
            inv.setItem(SLOT_CONFIRM, button("confirmButtonItem", Material.LIME_WOOL, "confirmButtonName", "&aConfirm delete", ACTION_CONFIRM_DELETE));
            inv.setItem(SLOT_CANCEL, button("cancelButtonItem", Material.RED_WOOL, "cancelButtonName", "&cCancel", ACTION_CANCEL_DELETE));
            player.openInventory(inv);
            return;
        }

        inv.setItem(SLOT_RENAME, button("renameButtonItem", Material.NAME_TAG, "renameButtonName", "Rename", ACTION_RENAME));
        inv.setItem(SLOT_MOVE, button("moveHomeButtonItem", Material.ENDER_PEARL, "moveHomeButtonName", "Move home here", ACTION_MOVE));
        inv.setItem(SLOT_ICON, button("setIconButtonItem", Material.ITEM_FRAME, "setIconButtonName", "Set icon to held item", ACTION_ICON));
        inv.setItem(SLOT_DELETE, button("deleteButtonItem", Material.BARRIER, "deleteButtonName", "&cDelete", ACTION_DELETE));
        inv.setItem(SLOT_BACK, button("backButtonItem", Material.ARROW, "backButtonName", "Back", ACTION_BACK));

        player.openInventory(inv);
    }

    public void setConfirmingDelete(boolean confirmingDelete) {
        this.confirmingDelete = confirmingDelete;
    }

    /**
     * Read the action tag off a clicked item.
     *
     * @param clicked The clicked item
     * @return The action constant, or null when the item is not a button
     */
    protected String actionOf(ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir() || clicked.getItemMeta() == null) return null;
        return clicked.getItemMeta().getPersistentDataContainer().get(actionKey(), new PersistentString());
    }

    @Override
    public void onClick(InventoryClickEvent event, GuiSession session) {
        Player player = (Player) event.getWhoClicked();
        String action = actionOf(event.getCurrentItem());

        if (action == null) return;

        if (ACTION_BACK.equals(action)) {
            session.openHomeList(player);
            return;
        }

        if (ACTION_MOVE.equals(action)) {
            Home fresh = reloadHome(player, session);
            if (fresh == null) return;

            Location destination = player.getLocation();
            String destinationDimension = Objects.requireNonNull(destination.getWorld()).getEnvironment().toString();

            // Blacklisted dimension guard, sharing the rule create-home applies.
            if (ServerUtil.isDimensionBlacklisted(destinationDimension)) {
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString("cannotMoveToBlacklistedDimension", UserError.CANNOT_MOVE_TO_BLACKLISTED_DIMENSION.getValue()));
                return;
            }

            fresh.setWorld(destination.getWorld().getUID().toString());
            fresh.setX(destination.getX());
            fresh.setY(destination.getY());
            fresh.setZ(destination.getZ());
            fresh.setPitch(destination.getPitch());
            fresh.setYaw(destination.getYaw());
            fresh.setDimension(destinationDimension);

            HomesDao homesDao = new HomesDao();
            if (!homesDao.update(fresh)) {
                ChatUtils.pluginError(player);
                return;
            }

            String moved = ConfigUtil.getConfig().getString("homeMoved", UserSuccess.HOME_MOVED.getValue());
            ChatUtils.sendSuccess(player, String.format(moved, fresh.getName()));
            returnToRefreshedList(player, session);
            return;
        }

        if (ACTION_ICON.equals(action)) {
            Home fresh = reloadHome(player, session);
            if (fresh == null) return;

            ItemStack held = player.getInventory().getItemInMainHand();

            if (held.getType().isAir()) {
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString("emptyHandForIcon", UserError.EMPTY_HAND_FOR_ICON.getValue()));
                return;
            }

            // Same validity rule create-home applies to a supplied material.
            if (!held.getType().isItem()) {
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString("invalidHomeItem", UserError.INVALID_MATERIAL.getValue()));
                return;
            }

            fresh.setMaterial(held.getType().name());

            HomesDao homesDao = new HomesDao();
            if (!homesDao.update(fresh)) {
                ChatUtils.pluginError(player);
                return;
            }

            String changed = ConfigUtil.getConfig().getString("homeIconChanged", UserSuccess.HOME_ICON_CHANGED.getValue());
            ChatUtils.sendSuccess(player, String.format(changed, fresh.getName(), held.getType().name()));
            returnToRefreshedList(player, session);
            return;
        }

        if (ACTION_DELETE.equals(action)) {
            setConfirmingDelete(true);
            displayInventory(player);
            return;
        }

        if (ACTION_CANCEL_DELETE.equals(action)) {
            setConfirmingDelete(false);
            displayInventory(player);
            return;
        }

        if (ACTION_CONFIRM_DELETE.equals(action)) {
            Home fresh = reloadHome(player, session);
            if (fresh == null) return;

            HomesDao homesDao = new HomesDao();
            if (!homesDao.delete(fresh)) {
                ChatUtils.pluginError(player);
                return;
            }

            String deleted = ConfigUtil.getConfig().getString("homeDeleted", UserSuccess.HOME_DELETED.getValue());
            ChatUtils.sendSuccess(player, String.format(deleted, fresh.getName()));
            returnToRefreshedList(player, session);
            return;
        }

        if (ACTION_RENAME.equals(action)) {
            Home fresh = reloadHome(player, session);
            if (fresh == null) return;

            openRenamePrompt(player, session, fresh);
            return;
        }
    }

    /**
     * Open an anvil prompt for the new home name. Validation failures re-prompt
     * with the reason rather than closing.
     *
     * @param player  The acting player
     * @param session The owning session
     * @param home    The home being renamed, freshly read
     */
    private void openRenamePrompt(Player player, GuiSession session, Home home) {
        SetHomesTwo plugin = SetHomesTwo.getPlugin(SetHomesTwo.class);
        int maxLength = ConfigUtil.getConfig().getInt("maxHomeNameLength", 32);

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(ConfigUtil.getConfig().getString("renamePromptTitle", "New home name"))
                .text(home.getName())
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String candidate = HomeNameValidator.normalise(stateSnapshot.getText());
                    HomeNameValidator.Result result = HomeNameValidator.validate(candidate, maxLength);

                    if (result == HomeNameValidator.Result.EMPTY) {
                        ChatUtils.sendError(player, ConfigUtil.getConfig().getString("invalidHomeName", UserError.INVALID_HOME_NAME.getValue()));
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(home.getName()));
                    }

                    if (result == HomeNameValidator.Result.TOO_LONG) {
                        String tooLong = ConfigUtil.getConfig().getString("homeNameTooLong", UserError.HOME_NAME_TOO_LONG.getValue());
                        ChatUtils.sendError(player, String.format(tooLong, maxLength));
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(home.getName()));
                    }

                    HomesDao homesDao = new HomesDao();

                    if (homesDao.nameExists(player.getUniqueId(), candidate, home.getId())) {
                        String duplicate = ConfigUtil.getConfig().getString("duplicateHomeName", UserError.DUPLICATE_HOME_NAME.getValue());
                        ChatUtils.sendError(player, String.format(duplicate, candidate));
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(home.getName()));
                    }

                    String previousName = home.getName();
                    home.setName(candidate);

                    if (!homesDao.update(home)) {
                        ChatUtils.pluginError(player);
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }

                    String renamed = ConfigUtil.getConfig().getString("homeRenamed", UserSuccess.HOME_RENAMED.getValue());
                    ChatUtils.sendSuccess(player, String.format(renamed, previousName, candidate));

                    return Arrays.asList(
                            AnvilGUI.ResponseAction.close(),
                            AnvilGUI.ResponseAction.run(() -> returnToRefreshedList(player, session))
                    );
                })
                .open(player);
    }

    /**
     * Re-read this menu's home from the database.
     *
     * @param player The acting player
     * @return The current home, or null when it no longer exists (the player has
     * already been messaged and returned to the list in that case)
     */
    private Home reloadHome(Player player, GuiSession session) {
        HomesDao homesDao = new HomesDao();
        Home fresh = homesDao.getById(player.getUniqueId(), homeId);

        if (fresh == null) {
            player.closeInventory();
            ChatUtils.sendError(player, ConfigUtil.getConfig().getString("homeNoLongerExists", UserError.HOME_NO_LONGER_EXISTS.getValue()));
            session.openHomeList(player);
            return null;
        }

        return fresh;
    }

    /**
     * Re-fetch the player's homes and show the refreshed list.
     *
     * @param player  The acting player
     * @param session The owning session
     */
    private void returnToRefreshedList(Player player, GuiSession session) {
        HomesDao homesDao = new HomesDao();
        session.getHomesGui().setHomes(homesDao.getAll(player.getUniqueId()));
        session.openHomeList(player);
    }
}

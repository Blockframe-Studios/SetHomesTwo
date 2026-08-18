package com.samleighton.sethomestwo.gui;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.datatypes.PersistentString;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.metrics.UsageCounters;
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
import java.util.Set;

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

    private static final Set<String> KNOWN_ACTIONS = Set.of(ACTION_RENAME, ACTION_MOVE, ACTION_ICON, ACTION_DELETE, ACTION_CONFIRM_DELETE, ACTION_CANCEL_DELETE, ACTION_BACK);

    private static final int SLOT_RENAME = 0;
    private static final int SLOT_MOVE = 1;
    private static final int SLOT_ICON = 2;
    private static final int SLOT_DELETE = 4;
    private static final int SLOT_BACK = 8;

    private static final int SLOT_CONFIRM = 2;
    private static final int SLOT_CANCEL = 6;

    /**
     * Why a rename attempt ended the way it did. Extracted so the decision tree
     * can be tested without opening an anvil, which needs NMS.
     */
    enum RenameOutcome {
        EMPTY,
        TOO_LONG,
        DUPLICATE,
        UPDATE_FAILED,
        RENAMED
    }

    /**
     * Why a move ended the way it did. Public so the move-home command, which
     * lives in a different package, can share the rule the GUI button applies.
     */
    public enum MoveOutcome {
        GONE,
        BLACKLISTED,
        UPDATE_FAILED,
        MOVED
    }

    private final Inventory inv;
    private final int homeId;
    private boolean confirmingDelete = false;

    public HomeActionsGui(Player player, Home home) {
        this.homeId = home.getId();

        String titleTemplate = ConfigUtil.getConfig().getString("manageHomeTitle", "Manage: %s");
        this.inv = Bukkit.createInventory(player, 9, String.format(titleTemplate, home.getName()));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private static NamespacedKey actionKey() {
        return new NamespacedKey(SetHomesTwo.instance(), ACTION_KEY_NAME);
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

        if (KNOWN_ACTIONS.contains(action)) SetHomesTwo.instance().getUsageCounters().increment(UsageCounters.Family.GUI_ACTION, action);

        if (ACTION_BACK.equals(action)) {
            session.openHomeList(player);
            return;
        }

        if (ACTION_MOVE.equals(action)) {
            Home fresh = reloadHome(player, session);
            if (fresh == null) return;

            switch (applyMove(player, fresh)) {
                case BLACKLISTED:
                    ChatUtils.sendError(player, ConfigUtil.getConfig().getString("cannotMoveToBlacklistedDimension", UserError.CANNOT_MOVE_TO_BLACKLISTED_DIMENSION.getValue()));
                    return;
                case UPDATE_FAILED:
                    ChatUtils.pluginError(player);
                    return;
                case MOVED:
                    String moved = ConfigUtil.getConfig().getString("homeMoved", UserSuccess.HOME_MOVED.getValue());
                    ChatUtils.sendSuccess(player, String.format(moved, fresh.getName()));
                    returnToRefreshedList(player, session);
                    return;
                default:
                    return;
            }
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
     * Validates and applies a new name, messaging the player on every outcome.
     */
    RenameOutcome applyRename(Player player, Home home, String rawName, int maxLength) {
        String candidate = HomeNameValidator.normalise(rawName);
        HomeNameValidator.Result result = HomeNameValidator.validate(candidate, maxLength);

        if (result == HomeNameValidator.Result.EMPTY) {
            ChatUtils.sendError(player, ConfigUtil.getConfig().getString("invalidHomeName", UserError.INVALID_HOME_NAME.getValue()));
            return RenameOutcome.EMPTY;
        }

        if (result == HomeNameValidator.Result.TOO_LONG) {
            String tooLong = ConfigUtil.getConfig().getString("homeNameTooLong", UserError.HOME_NAME_TOO_LONG.getValue());
            ChatUtils.sendError(player, String.format(tooLong, maxLength));
            return RenameOutcome.TOO_LONG;
        }

        HomesDao homesDao = new HomesDao();

        if (homesDao.nameExists(player.getUniqueId(), candidate, home.getId())) {
            String duplicate = ConfigUtil.getConfig().getString("duplicateHomeName", UserError.DUPLICATE_HOME_NAME.getValue());
            ChatUtils.sendError(player, String.format(duplicate, candidate));
            return RenameOutcome.DUPLICATE;
        }

        String previousName = home.getName();
        home.setName(candidate);

        if (!homesDao.update(home)) {
            ChatUtils.pluginError(player);
            return RenameOutcome.UPDATE_FAILED;
        }

        String renamed = ConfigUtil.getConfig().getString("homeRenamed", UserSuccess.HOME_RENAMED.getValue());
        ChatUtils.sendSuccess(player, String.format(renamed, previousName, candidate));
        return RenameOutcome.RENAMED;
    }

    /**
     * Moves a home to the player's current location, holding only the mutation:
     * no messaging, no navigation. Those stay with each caller.
     */
    public static MoveOutcome applyMove(Player player, Home home) {
        if (home == null) return MoveOutcome.GONE;

        Location destination = player.getLocation();
        if (!player.hasPermission("sh2.bypass-blacklist") && ServerUtil.isWorldBlacklisted(destination.getWorld()))
            return MoveOutcome.BLACKLISTED;

        home.setWorld(Objects.requireNonNull(destination.getWorld()).getUID().toString());
        home.setX(destination.getX());
        home.setY(destination.getY());
        home.setZ(destination.getZ());
        home.setPitch(destination.getPitch());
        home.setYaw(destination.getYaw());
        home.setDimension(destination.getWorld().getEnvironment().toString());

        return new HomesDao().update(home) ? MoveOutcome.MOVED : MoveOutcome.UPDATE_FAILED;
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
        SetHomesTwo plugin = SetHomesTwo.instance();
        int maxLength = ConfigUtil.getConfig().getInt("maxHomeNameLength", 32);

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(ConfigUtil.getConfig().getString("renamePromptTitle", "New home name"))
                .text(home.getName())
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    // Captured before the call, because a successful rename
                    // mutates the home's name in place.
                    String currentName = home.getName();
                    RenameOutcome outcome = applyRename(player, home, stateSnapshot.getText(), maxLength);

                    switch (outcome) {
                        case EMPTY:
                        case TOO_LONG:
                        case DUPLICATE:
                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(currentName));
                        case UPDATE_FAILED:
                            return Collections.singletonList(AnvilGUI.ResponseAction.close());
                        default:
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> Bukkit.getScheduler().runTask(plugin, () -> returnToRefreshedList(player, session)))
                            );
                    }
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
        player.closeInventory();
        HomesDao homesDao = new HomesDao(player.hasPermission("sh2.bypass-blacklist"));
        session.getHomesGui().setHomes(homesDao.getAll(player.getUniqueId()));
        session.openHomeList(player);
    }
}

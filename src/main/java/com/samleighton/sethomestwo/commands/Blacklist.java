package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.enums.PluginError;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Folds add-to-blacklist, remove-from-blacklist, and get-blacklisted-dimensions
 * into one executor. Bukkit only hands onCommand the label the player actually
 * typed (add-to-blacklist, remove-from-blacklist, get-blacklisted-dimensions, or
 * blacklist), so the subcommand is inferred from that label rather than always
 * reading args[0] - otherwise "/add-to-blacklist world_nether", the exact
 * pre-existing usage this command replaces, would silently fail.
 */
public class Blacklist implements CommandExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList("add", "remove", "list");

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;

        String subcommand;
        String[] names;

        switch (label.toLowerCase()) {
            case "add-to-blacklist":
                if (isExplicitSubcommand(args)) {
                    subcommand = args[0].toLowerCase();
                    names = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    subcommand = "add";
                    names = args;
                }
                break;
            case "remove-from-blacklist":
                if (isExplicitSubcommand(args)) {
                    subcommand = args[0].toLowerCase();
                    names = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    subcommand = "remove";
                    names = args;
                }
                break;
            case "get-blacklisted-dimensions":
                subcommand = "list";
                names = args;
                break;
            default:
                if (args.length < 1) {
                    ChatUtils.incorrectNumArguments(player);
                    ChatUtils.sendInfo(player, String.format(UserInfo.BLACKLIST_USAGE.getValue(), label));
                    return true;
                }
                subcommand = args[0].toLowerCase();
                names = Arrays.copyOfRange(args, 1, args.length);
                break;
        }

        switch (subcommand) {
            case "add":
                return add(player, label, names);
            case "remove":
                return remove(player, label, names);
            case "list":
                return list(player, label, names);
            default:
                ChatUtils.sendInfo(player, String.format(UserInfo.BLACKLIST_USAGE.getValue(), label));
                return true;
        }
    }

    /**
     * True when the alias's first argument is itself one of add/remove/list, so
     * "/add-to-blacklist add world_nether" is honoured instead of blacklisting a
     * world named "add".
     */
    private boolean isExplicitSubcommand(String[] args) {
        return args.length >= 1 && SUBCOMMANDS.contains(args[0].toLowerCase());
    }

    private boolean add(Player player, String label, String[] dimensions) {
        if (!player.hasPermission("sh2.add-to-blacklist")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        if (dimensions.length < 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, String.format(UserInfo.BLACKLIST_USAGE.getValue(), label));
            return true;
        }

        Dao<String> blacklistDao = new BlacklistDao();
        List<String> blacklistedDimensions = blacklistDao.getAll();

        for (String dimension : dimensions) {
            if (!ServerUtil.getValidDimensions().contains(dimension)) {
                ChatUtils.sendError(player, String.format(
                        ConfigUtil.getConfig().getString("invalidWorld", UserError.INVALID_WORLD.getValue()),
                        dimension, String.join(", ", ServerUtil.getValidDimensions())));
                continue;
            }

            if (blacklistedDimensions.contains(dimension)) {
                ChatUtils.sendError(player, String.format(UserError.DIMENSION_ALREADY_BLACKLISTED.getValue(), dimension));
                continue;
            }

            boolean success = blacklistDao.save(dimension);
            if (!success && ConfigUtil.getDebugLevel().equals(DebugLevel.INFO)) {
                Bukkit.getLogger().info(String.format("Failed to add dimension to blacklist. %s", dimension));
            }

            ChatUtils.sendSuccess(player, String.format(
                    ConfigUtil.getConfig().getString("dimensionAddedToBlacklist", UserSuccess.DIMENSION_ADDED_TO_BLACKLIST.getValue()),
                    dimension));
        }

        return true;
    }

    private boolean remove(Player player, String label, String[] dimensions) {
        if (!player.hasPermission("sh2.remove-from-blacklist")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        if (dimensions.length < 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, String.format(UserInfo.BLACKLIST_USAGE.getValue(), label));
            return true;
        }

        Dao<String> blacklistDao = new BlacklistDao();
        List<String> blacklistedDimensions = blacklistDao.getAll();

        for (String dimension : dimensions) {
            if (!ServerUtil.getValidDimensions().contains(dimension)) {
                ChatUtils.sendError(player, String.format(
                        ConfigUtil.getConfig().getString("invalidWorld", UserError.INVALID_WORLD.getValue()),
                        dimension, String.join(", ", ServerUtil.getValidDimensions())));
                continue;
            }

            if (!blacklistedDimensions.contains(dimension)) {
                ChatUtils.sendError(player, String.format(UserError.DIMENSION_IS_NOT_BLACKLISTED.getValue(), dimension));
                continue;
            }

            boolean success = blacklistDao.delete(dimension);
            if (!success && ConfigUtil.getDebugLevel().equals(DebugLevel.ERROR)) {
                Bukkit.getLogger().info(String.format("Failed to remove dimension from blacklist. %s", dimension));
                ChatUtils.sendError(player, PluginError.REMOVE_DIMENSION_FAILED.getValue());
            }

            ChatUtils.sendSuccess(player, String.format(
                    ConfigUtil.getConfig().getString("dimensionRemovedFromBlacklist", UserSuccess.DIMENSION_REMOVED_FROM_BLACKLIST.getValue()),
                    dimension
            ));
        }

        return true;
    }

    private boolean list(Player player, String label, String[] extraArgs) {
        if (!player.hasPermission("sh2.get-blacklisted-dimensions")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        if (extraArgs.length > 0) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, String.format(UserInfo.BLACKLIST_USAGE.getValue(), label));
            return true;
        }

        Dao<String> blacklistDao = new BlacklistDao();
        List<String> blacklistedDimensions = blacklistDao.getAll();

        if (blacklistedDimensions.isEmpty()) {
            ChatUtils.sendInfo(player, UserInfo.NO_BLACKLISTED_DIMENSIONS.getValue());
            return true;
        }

        String blacklist = "Blacklisted Dimensions: " + blacklistedDimensions;
        ChatUtils.sendInfo(player, blacklist);
        return true;
    }
}

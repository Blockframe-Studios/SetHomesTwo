package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DeletePlayerHome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player admin = (Player) commandSender;

        if (!admin.hasPermission("sh2.delete-player-home")) {
            ChatUtils.invalidPermissions(admin);
            return true;
        }

        if (args.length != 2) {
            ChatUtils.incorrectNumArguments(admin);
            ChatUtils.sendInfo(admin, UserInfo.DELETE_PLAYER_HOME_USAGE.getValue());
            return true;
        }

        String uuid = ServerUtil.getPlayerUUID(args[0]);

        if (uuid == null) {
            ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                    "playerNotFound", UserError.PLAYER_NOT_FOUND.getValue()));
            return true;
        }

        HomesDao homesDao = new HomesDao(true);
        Home home = homesDao.get(UUID.fromString(uuid), args[1]);

        if (home == null) {
            ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                    "homeNoLongerExists", UserError.HOME_NO_LONGER_EXISTS.getValue()));
            return true;
        }

        if (!homesDao.delete(home)) {
            ChatUtils.pluginError(admin);
            return true;
        }

        String deleted = ConfigUtil.getConfig().getString("playerHomeDeleted", UserSuccess.PLAYER_HOME_DELETED.getValue());
        ChatUtils.sendSuccess(admin, String.format(deleted, ownerName(home, args[0]), home.getName()));
        return true;
    }

    /**
     * The owner's name as stored against the home, which is canonically cased.
     * Falls back to what the admin typed for a home saved before names were
     * recorded.
     */
    private static String ownerName(Home home, String typed) {
        return home.getPlayerName() == null ? typed : home.getPlayerName();
    }
}

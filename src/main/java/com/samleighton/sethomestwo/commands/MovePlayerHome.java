package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.gui.HomeActionsGui;
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

public class MovePlayerHome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player admin = (Player) commandSender;

        if (!admin.hasPermission("sh2.move-player-home")) {
            ChatUtils.invalidPermissions(admin);
            return true;
        }

        if (args.length != 2) {
            ChatUtils.incorrectNumArguments(admin);
            ChatUtils.sendInfo(admin, UserInfo.MOVE_PLAYER_HOME_USAGE.getValue());
            return true;
        }

        String uuid = ServerUtil.getPlayerUUID(args[0]);

        if (uuid == null) {
            ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                    "playerNotFound", UserError.PLAYER_NOT_FOUND.getValue()));
            return true;
        }

        Home home = new HomesDao(true).get(UUID.fromString(uuid), args[1]);

        switch (HomeActionsGui.applyMove(admin, home)) {
            case GONE:
                ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                        "homeNoLongerExists", UserError.HOME_NO_LONGER_EXISTS.getValue()));
                break;
            case BLACKLISTED:
                ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                        "cannotMoveToBlacklistedDimension", UserError.CANNOT_MOVE_TO_BLACKLISTED_DIMENSION.getValue()));
                break;
            case UPDATE_FAILED:
                ChatUtils.pluginError(admin);
                break;
            case MOVED:
                String moved = ConfigUtil.getConfig().getString("playerHomeMoved", UserSuccess.PLAYER_HOME_MOVED.getValue());
                ChatUtils.sendSuccess(admin, String.format(moved, args[0], home.getName()));
                break;
        }

        return true;
    }
}

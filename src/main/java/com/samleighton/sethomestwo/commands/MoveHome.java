package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.gui.HomeActionsGui;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MoveHome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;

        if (!player.hasPermission("sh2.move-home")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        if (args.length != 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, UserInfo.MOVE_HOME_USAGE.getValue());
            return true;
        }

        Home home = new HomesDao().get(player.getUniqueId(), args[0]);

        switch (HomeActionsGui.applyMove(player, home)) {
            case GONE:
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString(
                        "homeNoLongerExists", UserError.HOME_NO_LONGER_EXISTS.getValue()));
                break;
            case BLACKLISTED:
                ChatUtils.sendError(player, ConfigUtil.getConfig().getString(
                        "cannotMoveToBlacklistedDimension", UserError.CANNOT_MOVE_TO_BLACKLISTED_DIMENSION.getValue()));
                break;
            case UPDATE_FAILED:
                ChatUtils.pluginError(player);
                break;
            case MOVED:
                String moved = ConfigUtil.getConfig().getString("homeMoved", UserSuccess.HOME_MOVED.getValue());
                ChatUtils.sendSuccess(player, String.format(moved, home.getName()));
                break;
        }

        return true;
    }
}

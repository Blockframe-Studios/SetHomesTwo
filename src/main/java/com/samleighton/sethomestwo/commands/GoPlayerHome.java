package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
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

public class GoPlayerHome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player admin = (Player) commandSender;

        if (!admin.hasPermission("sh2.go-player-home")) {
            ChatUtils.invalidPermissions(admin);
            return true;
        }

        if (args.length != 2) {
            ChatUtils.incorrectNumArguments(admin);
            ChatUtils.sendInfo(admin, UserInfo.GO_PLAYER_HOME_USAGE.getValue());
            return true;
        }

        String uuid = ServerUtil.getPlayerUUID(args[0]);

        if (uuid == null) {
            ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                    "playerNotFound", UserError.PLAYER_NOT_FOUND.getValue()));
            return true;
        }

        // Unlike getAll, get applies no blacklist rule, so canTeleport stays at
        // its default and an admin reaches the home whatever world it is in.
        Home home = new HomesDao(true).get(UUID.fromString(uuid), args[1]);

        if (home == null) {
            ChatUtils.sendError(admin, String.format(ConfigUtil.getConfig().getString(
                    "homeDoesNotExist", UserError.HOME_DOES_NOT_EXIST.getValue()), args[1]));
            return true;
        }

        home.teleport(admin);
        return true;
    }
}

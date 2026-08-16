package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.metrics.UsageCounters;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.Bukkit;
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
            ChatUtils.sendInfo(admin, String.format(UserInfo.GO_PLAYER_HOME_USAGE.getValue(), label));
            return true;
        }

        String uuid = ServerUtil.getPlayerUUID(args[0]);

        if (uuid == null) {
            ChatUtils.sendError(admin, ConfigUtil.getConfig().getString(
                    "playerNotFound", UserError.PLAYER_NOT_FOUND.getValue()));
            return true;
        }

        Home home = new HomesDao(true).get(UUID.fromString(uuid), args[1]);

        if (home == null) {
            ChatUtils.sendError(admin, String.format(ConfigUtil.getConfig().getString(
                    "homeDoesNotExist", UserError.HOME_DOES_NOT_EXIST.getValue()), args[1]));
            return true;
        }

        // get applies no blacklist rule of its own, unlike getAll, so the node
        // has to be honoured here or it could not be taken away. Clearing the
        // flag lets Home.teleport send the same refusal a player would see.
        if (!admin.hasPermission("sh2.bypass-blacklist")
                && ServerUtil.isWorldBlacklisted(Bukkit.getWorld(UUID.fromString(home.getWorld())))) {
            home.setCanTeleport(false);
        }

        SetHomesTwo.instance().getUsageCounters().increment(UsageCounters.Family.TELEPORT_SOURCE, UsageCounters.SOURCE_COMMAND);
        home.teleport(admin);
        return true;
    }
}

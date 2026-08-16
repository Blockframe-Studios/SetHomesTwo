package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.metrics.UsageCounters;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.HomesUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class GoHome implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Guard for player being console command sender
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;

        // Args length guard. A bare command is the v1 form, so only too many
        // arguments is an error.
        if(args.length > 1){
            ChatUtils.incorrectNumArguments(player);
            return true;
        }

        // Permission guard
        if(!player.hasPermission("sh2.go-home")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        // Get players home dao instance
        String desiredHomeName = args.length < 1 ? HomesUtil.DEFAULT_HOME_NAME : args[0];
        Dao<Home> homesDao = new HomesDao(player.hasPermission("sh2.bypass-blacklist"));
        ArrayList<Home> playerHomes = (ArrayList<Home>) homesDao.getAll(player.getUniqueId());
        Home homeToTeleportTo = null;

        // Check for and obtain home instance if it exists
        for(Home home : playerHomes) {
            if(home.getName().equalsIgnoreCase(desiredHomeName)){
                homeToTeleportTo = home;
            }
        }

        // Home does not exist guard
        if(homeToTeleportTo == null){
            String message = ConfigUtil.getConfig().getString("homeDoesNotExist", UserError.HOME_DOES_NOT_EXIST.getValue());
            ChatUtils.sendError(player, String.format(message, desiredHomeName));
            return true;
        }

        // Teleport player to home
        SetHomesTwo.instance().getUsageCounters().increment(UsageCounters.Family.TELEPORT_SOURCE, UsageCounters.SOURCE_COMMAND);
        homeToTeleportTo.teleport(player);
        return true;
    }
}

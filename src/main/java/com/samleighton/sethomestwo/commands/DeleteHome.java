package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DeleteHome implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;

        // Permission guard
        if(!player.hasPermission("sh2.delete-home")){
            ChatUtils.invalidPermissions(player);
            return true;
        }

        // Args length guard
        if (args.length != 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, String.format(UserError.DELETE_HOME_USAGE.getValue(), s));
            return true;
        }

        String homeName = args[0];
        Dao<Home> homesDao = new HomesDao();
        Home home = homesDao.get(player.getUniqueId(), homeName);

        // Home does not exist guard
        if(home == null){
            ChatUtils.sendError(player, String.format("You do not have a home by the name %s", homeName));
            return true;
        }

        boolean success = homesDao.delete(home);

        // Guard for successful home deletion
        if (!success) {
            Bukkit.getLogger().info("An error was encountered while attempting to delete a home from the database.");
            ChatUtils.pluginError(player);
            return true;
        }

        String successMessage = ConfigUtil.getConfig().getString("homeDeleted", UserSuccess.HOME_DELETED.getValue());
        ChatUtils.sendSuccess(player, String.format(successMessage, homeName));
        return true;
    }
}

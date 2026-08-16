package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.enums.PluginError;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class SetMaxHomes implements CommandExecutor {
    private final Plugin plugin;

    public SetMaxHomes(Plugin plugin) {
        this.plugin = plugin;
    }
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return false;
        }

        // Load in config.yml
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config =  YamlConfiguration.loadConfiguration(configFile);

        Player player = (Player) commandSender;
        String maxHomesType = config.getString("maxHomesType", "singular");

        // Check if max homes is even set
        if (config.getString("maxHomes") == null) {
            ChatUtils.sendInfo(player, UserInfo.NO_MAX_HOMES.getValue());
            return false;
        }

        // Depending on if grouping is singular or groups, guard against incorrect number of args
        if (maxHomesType.equals("singular") && args.length != 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendError(player, String.format(UserError.SET_MAX_HOMES_SINGULAR.getValue(), label));
            return false;
        }

        if (maxHomesType.equals("groups") && args.length != 2) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendError(player, String.format(UserError.SET_MAX_HOMES_GROUPS.getValue(), label));
            return false;
        }

        if (args.length == 1) {
            int maxHomesNum = Integer.parseInt(args[0]);
            config.set("maxHomes", maxHomesNum);
        } else {
            String group = "maxHomes." + args[0];
            // Check if group exists
            if (config.getString(group) != null) {
                int maxHomesNum = Integer.parseInt(args[1]);
                config.set(group, maxHomesNum);
            } else {
                ChatUtils.sendError(player,UserError.GROUP_DOES_NOT_EXIST.getValue());
                return false;
            }
        }

        // Save config file
        try {
            config.save(configFile);
            ChatUtils.sendSuccess(player, UserSuccess.MAX_HOMES_UPDATED_SUCCESSFULLY.getValue());
        } catch (IOException e) {
            ChatUtils.sendError(player, PluginError.MAX_HOMES_UPDATE_FAILED.getValue());
            Bukkit.getLogger().severe("Error writing to config file while updating max homes settings!");
        }

        return false;
    }
}

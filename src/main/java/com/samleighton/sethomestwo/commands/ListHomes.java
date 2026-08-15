package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListHomes implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Players only guard
        if (!(sender instanceof Player)) {
            ChatUtils.sendError(sender, UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) sender;

        // Permission guard
        if (!player.hasPermission("sh2.list-homes")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        Dao<Home> homesDao = new HomesDao(player.hasPermission("sh2.bypass-blacklist"));
        List<Home> playersHomes = homesDao.getAll(player.getUniqueId());

        // Player has no homes guard
        if (playersHomes.isEmpty()) {
            ChatUtils.sendInfo(player, UserInfo.NO_HOMES.getValue());
            ChatUtils.sendInfo(player, UserInfo.CREATE_HOME_USAGE.getValue());
            return true;
        }

        // Start printing players homes
        String filler = StringUtils.repeat('-', 53);

        player.sendMessage(ChatColor.BOLD + "Your currently set homes");
        player.sendMessage(filler);

        for (Home home : playersHomes) {
            // Obtain world instance from world UUID
            World bukkitWorld = Bukkit.getWorld(UUID.fromString(home.getWorld()));

            // Init main message object
            TextComponent mainMessage = new TextComponent();

            // Build all message pieces
            TextComponent homeName = new TextComponent("Name: ");
            homeName.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);

            TextComponent homeWorld = new TextComponent("World: ");
            homeWorld.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);

            TextComponent separator = new TextComponent(" | ");
            separator.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);

            TextComponent name = new TextComponent(home.getName());
            name.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + home.getName()));
            name.setUnderlined(true);

            TextComponent world = new TextComponent(Objects.requireNonNull(bukkitWorld).getName());

            // Add home name to main message
            mainMessage.addExtra(homeName);
            mainMessage.addExtra(name);
            mainMessage.addExtra(separator);

            // Add home world to main message
            mainMessage.addExtra(homeWorld);
            mainMessage.addExtra(world);
            mainMessage.addExtra(separator);

            // Add on description to main message if provided
            if (home.getDescription() != null && !home.getDescription().isEmpty()) {
                TextComponent homeDescription = new TextComponent("Desc: ");
                homeDescription.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);

                TextComponent desc = new TextComponent(home.getDescription());

                mainMessage.addExtra(homeDescription);
                mainMessage.addExtra(desc);
            }

            // Send final text component to player with clickable home name to teleport.
            player.spigot().sendMessage(mainMessage);
        }

        // End message block with filler content
        player.sendMessage(filler);

        return true;
    }
}

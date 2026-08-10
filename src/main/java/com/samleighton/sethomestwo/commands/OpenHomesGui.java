package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OpenHomesGui implements CommandExecutor {

    private final SetHomesTwo plugin;

    public OpenHomesGui(SetHomesTwo plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        // Ensure command executor is a player
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;

        // Permission guard
        if (!player.hasPermission("sh2.list-homes")) {
            ChatUtils.invalidPermissions(player);
            return true;
        }

        Dao<Home> homesDao = new HomesDao();
        List<Home> playersHomes = homesDao.getAll(player.getUniqueId());

        // Guard for no homes yet
        if (playersHomes == null || playersHomes.isEmpty()) {
            ChatUtils.sendInfo(player, ConfigUtil.getConfig().getString("noHomes", UserError.NO_HOMES.getValue()));
            return true;
        }

        // The join listener normally seeds this map; compute a fresh gui if absent
        // (e.g. plugin reloaded while the player was online).
        HomesGui homesGui = plugin.getHomesGuiMap().computeIfAbsent(player.getUniqueId(), uuid -> new HomesGui(player));
        homesGui.setHomes(playersHomes);
        homesGui.displayInventory(player);
        return true;
    }
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
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

import java.util.List;
import java.util.UUID;

public class GetPlayerHomes implements CommandExecutor {

    private final SetHomesTwo plugin;

    public GetPlayerHomes(SetHomesTwo plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        // Player instance guard
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player requester = (Player) commandSender;

        // Permission guard
        if (!requester.hasPermission("sh2.get-player-homes")) {
            ChatUtils.invalidPermissions(requester);
            return true;
        }

        // Args length guard
        if (args.length != 1) {
            ChatUtils.incorrectNumArguments(requester);
            ChatUtils.sendError(requester, UserInfo.GET_PLAYER_HOMES_USAGE.getValue());
            return true;
        }

        String uuidString = ServerUtil.getPlayerUUID(args[0]);

        // null means the name matched no online player and no stored home owner
        if (uuidString == null) {
            ChatUtils.sendError(requester, ConfigUtil.getConfig().getString(
                    "playerNotFound", UserError.PLAYER_NOT_FOUND.getValue()));
            return true;
        }

        Dao<Home> homesDao = new HomesDao(true);
        List<Home> playersHomes = homesDao.getAll(UUID.fromString(uuidString));

        Player target = Bukkit.getPlayer(UUID.fromString(uuidString));
        String targetName = target == null ? args[0] : target.getDisplayName();

        HomesGui adminGui = new HomesGui(requester, "Homes of " + targetName);
        adminGui.setHomes(playersHomes);

        GuiSession session = plugin.getGuiSessionMap().computeIfAbsent(requester.getUniqueId(), uuid -> new GuiSession(new HomesGui(requester)));
        session.setActiveScreen(adminGui);
        adminGui.displayInventory(requester);

        if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
            Bukkit.getLogger().info(String.format("%s is viewing homes of player %s", requester.getDisplayName(), targetName));

        return true;
    }
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.HomesUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CreateHome implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        // Ensure command executor is a player
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(UserError.PLAYERS_ONLY.getValue());
            return true;
        }

        Player player = (Player) commandSender;
        Location playerLocation = player.getLocation();
        Dao<Home> homesDao = new HomesDao();

        // Permission guard
        if(!player.hasPermission("sh2.create-home")){
            ChatUtils.invalidPermissions(player);
            return true;
        }

        // Guard to ensure we have minimum number of args
        if (args.length < 1) {
            ChatUtils.incorrectNumArguments(player);
            ChatUtils.sendInfo(player, UserInfo.CREATE_HOME_USAGE.getValue());
            return true;
        }

        // Guard to check if player has exceeded the max number of homes
        if (this.maxHomesReached(player, homesDao)){
            String errorMessage = ConfigUtil.getConfig().getString("maxHomesReached", UserError.MAX_HOMES.getValue());
            ChatUtils.sendError(player, errorMessage);
            return true;
        }

        String playerDimension = player.getWorld().getEnvironment().toString();

        // Check if player is in a blacklisted dimension before creating home
        if (ServerUtil.isWorldBlacklisted(player.getWorld())) {
            String errorMessage = ConfigUtil.getConfig().getString("dimensionBlacklisted", UserError.DIMENSION_IS_BLACKLISTED.getValue());
            ChatUtils.sendError(player, errorMessage);
            return true;
        }

        // Extract parameters from command arguments
        String homeName = args[0];

        String material = "";
        if (args.length > 1)
            material = args[1];

        // Guard to ensure material entered is a valid material
        boolean isMaterialBlankOrDefault = material.equalsIgnoreCase("d") || material.equalsIgnoreCase("default") || material.equalsIgnoreCase("");
        Material mat = isMaterialBlankOrDefault ? Material.WHITE_WOOL : Material.matchMaterial(material);
        if (mat == null) {
            String errorMessage = ConfigUtil.getConfig().getString("invalidHomeItem", UserError.INVALID_MATERIAL.getValue());
            ChatUtils.sendError(player, errorMessage);
            return true;
        }
        if (!mat.isItem()) {
            String errorMessage = ConfigUtil.getConfig().getString("invalidHomeItem", UserError.INVALID_MATERIAL.getValue());
            ChatUtils.sendError(player, errorMessage);
            return true;
        }

        material = mat.name();
        String description = null;
        StringBuilder stringBuilder = new StringBuilder();

        // Build description from leftover arguments
        if (args.length > 2) {
            String[] remainingArgs = Arrays.copyOfRange(args, 2, args.length);
            for (int i = 0; i < remainingArgs.length; i++) {
                String arg = remainingArgs[i];
                if (i == remainingArgs.length - 1) {
                    stringBuilder.append(arg);
                } else {
                    stringBuilder.append(arg).append(" ");
                }
            }

            description = stringBuilder.toString();
        }

        // Duplicate name guard
        HomesDao homesLookup = new HomesDao();
        if (homesLookup.nameExists(player.getUniqueId(), homeName, null)) {
            String duplicateMessage = ConfigUtil.getConfig().getString("duplicateHomeName", UserError.DUPLICATE_HOME_NAME.getValue());
            ChatUtils.sendError(player, String.format(duplicateMessage, homeName));
            return true;
        }

        // Create the home
        boolean created = homesDao.save(new Home(
                player.getUniqueId().toString(),
                material,
                playerLocation,
                homeName,
                description,
                playerDimension
        ));

        if (!created) {
            Bukkit.getLogger().severe(String.format("Failed to create home for player %s in the database.", player.getUniqueId()));
            ChatUtils.sendError(player, "There was an issue creating your home.");
            return true;
        }

        String message = ConfigUtil.getConfig().getString("homeCreated", UserSuccess.HOME_CREATED.getValue());
        ChatUtils.sendSuccess(player, String.format(message, homeName));
        return true;
    }

    private boolean maxHomesReached(Player player, Dao<Home> homesDao){
        boolean isMaxHomesEnabled = ConfigUtil.getConfig().getBoolean("maxHomeEnabled", false);
        if (!isMaxHomesEnabled) return false;

        String maxHomesType = ConfigUtil.getConfig().getString("maxHomesType", "singular");
        int maxHomesAllowed = -1;

        switch (maxHomesType){
            case "singular":
                maxHomesAllowed = ConfigUtil.getConfig().getInt("maxHomes", -1);
                break;
            case "groups":
                // LuckPerms is a soft dependency; without it group limits cannot be resolved
                if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                    Bukkit.getLogger().warning("maxHomesType is 'groups' but LuckPerms is not installed. Max homes limit will not be enforced.");
                    break;
                }
                ConfigurationSection maxHomesSection = ConfigUtil.getConfig().getConfigurationSection("maxHomes");
                Map<String, Integer> maxHomesMap = new HashMap<>();
                for(String key : Objects.requireNonNull(maxHomesSection).getKeys(false)){
                    maxHomesMap.put(key, ConfigUtil.getConfig().getInt("maxHomes."+key));
                }

                if (maxHomesMap.isEmpty()) break;

                LuckPerms lpApi = LuckPermsProvider.get();
                User user = lpApi.getUserManager().getUser(player.getUniqueId());
                String primaryGroup = Objects.requireNonNull(user).getPrimaryGroup();
                if (!maxHomesMap.containsKey(primaryGroup)) break;

                maxHomesAllowed = maxHomesMap.get(primaryGroup);
                break;
        }

        if (maxHomesAllowed == -1) return false;

        int playersHomeCount = HomesUtil.getPlayerHomesCount(homesDao, player.getUniqueId());
        return playersHomeCount >= maxHomesAllowed;
    }
}

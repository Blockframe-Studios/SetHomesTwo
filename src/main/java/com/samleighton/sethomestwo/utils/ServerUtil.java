package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.Dao;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerUtil {

    /**
     * Retrieve a list of the server's valid dimensions.
     * <p>
     * Read live rather than cached in a static initialiser: the cached form
     * bound itself to whichever server loaded the class first, and threw
     * outright on a server with fewer than three worlds.
     *
     * @return List
     */
    public static List<String> getValidDimensions() {
        List<String> validDimensions = new ArrayList<>();
        Bukkit.getWorlds().forEach(world -> validDimensions.add(world.getName().toLowerCase()));

        return validDimensions;
    }

    /**
     * Retrieve dimensions mapping.
     * <p>
     * Worlds are matched by position, so a server that disables the Nether or
     * the End simply contributes no entry for it rather than failing.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> getDimensionsMap() {
        List<String> validDimensions = getValidDimensions();
        Map<String, String> dimensionsMap = new HashMap<>();

        if (validDimensions.size() > 0) dimensionsMap.put("NORMAL", validDimensions.get(0));
        if (validDimensions.size() > 1) dimensionsMap.put("NETHER", validDimensions.get(1));
        if (validDimensions.size() > 2) dimensionsMap.put("THE_END", validDimensions.get(2));

        return dimensionsMap;
    }

    /**
     * Whether homes are barred from a dimension.
     *
     * @param dimension The environment name, as produced by
     *                  world.getEnvironment().toString()
     * @return true when the dimension is blacklisted
     */
    public static boolean isDimensionBlacklisted(String dimension) {
        Dao<String> blacklistDao = new BlacklistDao();
        List<String> blacklistedDimensions = blacklistDao.getAll();

        return blacklistedDimensions.contains(getDimensionsMap().get(dimension));
    }

    public static String getPlayerUUID(String playerName){
        for(Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getDisplayName();
            if(name.equals(playerName)) {
                return player.getUniqueId().toString();
            }
        }

        return null;
    }
}

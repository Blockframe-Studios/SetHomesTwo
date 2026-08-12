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
    private final static List<String> validDimensions = new ArrayList<>() {
        {
            Bukkit.getWorlds().forEach(world -> add(world.getName().toLowerCase()));
        }
    };

    // Mapping the environment grabbed from player to our valid dimension list
    private final static Map<String, String> dimensionsMap = new HashMap<>() {{
        put("NORMAL", validDimensions.get(0));
        put("NETHER", validDimensions.get(1));
        put("THE_END", validDimensions.get(2));
    }};

    /**
     * Retrieve a list of the server's valid dimensions.
     *
     * @return List
     */
    public static List<String> getValidDimensions() {
        return validDimensions;
    }

    /**
     * Retrieve dimensions mapping.
     *
     * @return Map<String, String>
     */
    public static Map<String, String> getDimensionsMap() {
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

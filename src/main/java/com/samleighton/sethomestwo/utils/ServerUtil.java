package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.HomesDao;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerUtil {

    /**
     * Retrieve a list of the server's valid dimensions.
     * <p>
     * Read live, not cached - the cached form bound to whichever server loaded
     * the class first and threw on fewer than three worlds.
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
     * Worlds are matched by list position, so this is only correct when all three
     * dimensions exist in the usual order. The size checks prevent a crash on a
     * smaller server, not a wrong mapping. Pre-existing limitation.
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
     * Whether homes are barred from a world. Blacklist rows hold lowercased
     * world names, so the world is compared directly rather than through an
     * environment mapping, which could only ever address the first three worlds.
     *
     * @param world The world to check
     * @return true when the world is blacklisted
     */
    public static boolean isWorldBlacklisted(World world) {
        Dao<String> blacklistDao = new BlacklistDao();
        return isWorldBlacklisted(world, blacklistDao.getAll());
    }

    /**
     * Same rule as {@link #isWorldBlacklisted(World)}, against an
     * already-fetched blacklist so a caller checking many worlds does not
     * query once per check.
     *
     * @param world             The world to check
     * @param blacklistedWorlds Lowercased world names, as returned by
     *                          {@code new BlacklistDao().getAll()}
     * @return true when the world is blacklisted
     */
    public static boolean isWorldBlacklisted(World world, List<String> blacklistedWorlds) {
        if (world == null) return false;

        return blacklistedWorlds.contains(world.getName().toLowerCase());
    }

    /**
     * Resolve a player name to a UUID, falling back to the names stored against
     * saved homes so offline players can be addressed. The match ignores case,
     * which is safe because Minecraft names are unique ignoring case.
     */
    public static String getPlayerUUID(String playerName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player.getUniqueId().toString();
            }
        }

        return new HomesDao().uuidForName(playerName);
    }
}

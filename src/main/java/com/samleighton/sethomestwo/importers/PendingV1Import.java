package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Whether a Set Homes v1 homes.yml is sitting there waiting to be imported.
 * Reads the same file and layout as {@link SetHomesV1Importer}.
 */
public final class PendingV1Import {

    public static final String SOURCE_PATH = "plugins/SetHomes/homes.yml";

    private PendingV1Import() {
    }

    /**
     * Homes waiting in v1's file while our own database is still empty, or 0
     * when there is nothing to announce. The database is checked first, so a
     * server that has already imported never touches the disk.
     *
     * @return the number of homes waiting, 0 if none or if we already hold homes
     */
    public static int waitingToBeImported() {
        if (new HomesDao().countAll() > 0) return 0;

        File homesFile = new File(SetHomesTwo.instance().getDataFolder().getParentFile(), "SetHomes/homes.yml");
        if (!homesFile.exists()) return 0;

        return countHomes(YamlConfiguration.loadConfiguration(homesFile));
    }

    private static int countHomes(YamlConfiguration source) {
        int total = 0;

        ConfigurationSection allNamed = source.getConfigurationSection("allNamedHomes");
        if (allNamed != null) {
            for (String uuid : allNamed.getKeys(false)) {
                ConfigurationSection playerSection = allNamed.getConfigurationSection(uuid);
                if (playerSection != null) total += playerSection.getKeys(false).size();
            }
        }

        // One unnamed home per player, which the importer brings across as "default".
        ConfigurationSection unknown = source.getConfigurationSection("unknownHomes");
        if (unknown != null) total += unknown.getKeys(false).size();

        return total;
    }
}

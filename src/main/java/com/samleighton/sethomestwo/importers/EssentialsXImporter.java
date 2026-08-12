package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public class EssentialsXImporter implements HomesImporter {

    @Override
    public String sourceName() {
        return "essentialsx";
    }

    @Override
    public ImportReport run(boolean dryRun) {
        ImportReport report = new ImportReport();

        File pluginsDir = SetHomesTwo.instance().getDataFolder().getParentFile();
        File userdataDir = new File(pluginsDir, "Essentials/userdata");
        File[] userFiles = userdataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (userFiles == null || userFiles.length == 0) {
            report.warnings.add("plugins/Essentials/userdata contains no player files - nothing to import.");
            return report;
        }

        HomesDao homesDao = new HomesDao();

        for (File userFile : userFiles) {
            String playerUUID = userFile.getName().substring(0, userFile.getName().length() - 4);
            try {
                UUID.fromString(playerUUID);
            } catch (IllegalArgumentException e) {
                continue; // not a player userdata file
            }

            YamlConfiguration userData = YamlConfiguration.loadConfiguration(userFile);
            ConfigurationSection homes = userData.getConfigurationSection("homes");
            if (homes == null) continue;

            for (String homeName : homes.getKeys(false)) {
                importOne(homesDao, report, homes.getConfigurationSection(homeName), playerUUID, homeName, dryRun);
            }
        }

        return report;
    }

    private void importOne(HomesDao homesDao, ImportReport report, ConfigurationSection home, String playerUUID, String homeName, boolean dryRun) {
        try {
            if (home == null) {
                report.failed++;
                return;
            }

            World world = resolveWorld(home);
            if (world == null) {
                report.skippedWorldMissing++;
                report.warnings.add(String.format("World for home '%s' of player %s does not exist on this server.", homeName, playerUUID));
                return;
            }

            if (homesDao.get(UUID.fromString(playerUUID), homeName) != null) {
                report.skippedExisting++;
                return;
            }

            Location location = new Location(
                    world,
                    home.getDouble("x"),
                    home.getDouble("y"),
                    home.getDouble("z"),
                    (float) home.getDouble("yaw"),
                    (float) home.getDouble("pitch")
            );

            if (!dryRun) {
                boolean saved = homesDao.save(new Home(
                        playerUUID,
                        HomesImporter.defaultMaterial(),
                        location,
                        homeName,
                        null,
                        world.getEnvironment().toString()
                ));
                if (!saved) {
                    report.failed++;
                    return;
                }
            }

            report.imported++;
        } catch (Exception e) {
            report.failed++;
            report.warnings.add(String.format("Home '%s' for player %s could not be read: %s", homeName, playerUUID, e.getMessage()));
        }
    }

    /** Modern files: world = world UUID, world-name = name. Legacy: world = name. */
    private World resolveWorld(ConfigurationSection home) {
        String worldValue = home.getString("world");
        if (worldValue != null) {
            try {
                World byId = Bukkit.getWorld(UUID.fromString(worldValue));
                if (byId != null) return byId;
            } catch (IllegalArgumentException ignored) {
                World byName = Bukkit.getWorld(worldValue);
                if (byName != null) return byName;
            }
        }

        String worldName = home.getString("world-name");
        return worldName == null ? null : Bukkit.getWorld(worldName);
    }
}

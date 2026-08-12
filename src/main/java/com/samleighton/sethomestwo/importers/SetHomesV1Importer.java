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

public class SetHomesV1Importer implements HomesImporter {

    @Override
    public String sourceName() {
        return "sethomes";
    }

    @Override
    public ImportReport run(boolean dryRun) {
        ImportReport report = new ImportReport();

        File pluginsDir = SetHomesTwo.instance().getDataFolder().getParentFile();
        File homesFile = new File(pluginsDir, "SetHomes/homes.yml");
        if (!homesFile.exists()) {
            report.warnings.add("plugins/SetHomes/homes.yml not found - nothing to import.");
            return report;
        }

        YamlConfiguration source = YamlConfiguration.loadConfiguration(homesFile);
        HomesDao homesDao = new HomesDao();

        // Named homes: allNamedHomes.<uuid>.<name>.{world,x,y,z,pitch,yaw,desc}
        ConfigurationSection allNamed = source.getConfigurationSection("allNamedHomes");
        if (allNamed != null) {
            for (String uuid : allNamed.getKeys(false)) {
                ConfigurationSection playerSection = allNamed.getConfigurationSection(uuid);
                if (playerSection == null) continue;
                for (String homeName : playerSection.getKeys(false)) {
                    importOne(homesDao, report, playerSection.getConfigurationSection(homeName), uuid, homeName, dryRun);
                }
            }
        }

        // Unnamed default homes: unknownHomes.<uuid>.{world,x,y,z,pitch,yaw}
        ConfigurationSection unknown = source.getConfigurationSection("unknownHomes");
        if (unknown != null) {
            for (String uuid : unknown.getKeys(false)) {
                importOne(homesDao, report, unknown.getConfigurationSection(uuid), uuid, "default", dryRun);
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

            World world = Bukkit.getWorld(String.valueOf(home.getString("world")));
            if (world == null) {
                report.skippedWorldMissing++;
                report.warnings.add(String.format("World '%s' (home '%s') does not exist on this server.", home.getString("world"), homeName));
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
                        home.getString("desc"),
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
}

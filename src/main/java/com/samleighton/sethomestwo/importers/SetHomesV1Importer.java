package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
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

        importBlacklist(pluginsDir, report, dryRun);

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

            String playerName = HomesImporter.resolveCachedName(UUID.fromString(playerUUID));
            if (playerName != null) report.namesResolved++;

            if (!dryRun) {
                Home importedHome = new Home(
                        playerUUID,
                        HomesImporter.defaultMaterial(),
                        location,
                        homeName,
                        home.getString("desc"),
                        world.getEnvironment().toString()
                );
                importedHome.setPlayerName(playerName);

                boolean saved = homesDao.save(importedHome);
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

    /**
     * v1's world_blacklist.yml holds a flat blacklisted_worlds list. Missing or
     * empty is normal (v1 shipped it empty by default), not an error. A world
     * absent from this server is still stored - it is harmless to block a world
     * that does not exist - but is called out with a warning in case the name
     * was a typo.
     */
    private void importBlacklist(File pluginsDir, ImportReport report, boolean dryRun) {
        File blacklistFile = new File(pluginsDir, "SetHomes/world_blacklist.yml");
        if (!blacklistFile.exists()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(blacklistFile);
        List<String> worlds = source.getStringList("blacklisted_worlds");
        if (worlds.isEmpty()) return;

        BlacklistDao blacklistDao = new BlacklistDao();
        List<String> existing = blacklistDao.getAll();

        for (String world : worlds) {
            String lowered = world.toLowerCase();

            if (existing.contains(lowered)) {
                report.blacklistSkippedExisting++;
                continue;
            }

            if (Bukkit.getWorld(lowered) == null) {
                report.warnings.add(String.format("Blacklisted world '%s' does not exist on this server.", lowered));
            }

            if (!dryRun) {
                blacklistDao.save(lowered);
            }

            // Tracked locally too, so a world repeated in the source file is
            // only ever counted (and written) once per run.
            existing.add(lowered);
            report.blacklistImported++;
        }
    }
}

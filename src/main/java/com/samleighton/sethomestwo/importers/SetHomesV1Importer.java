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
import java.util.Map;
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
        // Snapshotted once, not per home - Bukkit.getOfflinePlayers() scans the
        // server's playerdata directory, which is expensive to re-run for every
        // imported home on a server with years of accumulated players.
        Map<UUID, String> cachedNames = HomesImporter.cachedNames();

        // Named homes: allNamedHomes.<uuid>.<name>.{world,x,y,z,pitch,yaw,desc}
        ConfigurationSection allNamed = source.getConfigurationSection("allNamedHomes");
        if (allNamed != null) {
            for (String uuid : allNamed.getKeys(false)) {
                ConfigurationSection playerSection = allNamed.getConfigurationSection(uuid);
                if (playerSection == null) continue;
                for (String homeName : playerSection.getKeys(false)) {
                    importOne(homesDao, report, playerSection.getConfigurationSection(homeName), uuid, homeName, cachedNames, dryRun);
                }
            }
        }

        // Unnamed default homes: unknownHomes.<uuid>.{world,x,y,z,pitch,yaw}
        ConfigurationSection unknown = source.getConfigurationSection("unknownHomes");
        if (unknown != null) {
            for (String uuid : unknown.getKeys(false)) {
                importOne(homesDao, report, unknown.getConfigurationSection(uuid), uuid, "default", cachedNames, dryRun);
            }
        }

        importBlacklist(pluginsDir, report, dryRun);
        reportConfig(pluginsDir, report);

        return report;
    }

    private void importOne(HomesDao homesDao, ImportReport report, ConfigurationSection home, String playerUUID, String homeName, Map<UUID, String> cachedNames, boolean dryRun) {
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

            String playerName = cachedNames.get(UUID.fromString(playerUUID));
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
     * was a typo. That warning also flags that /remove-from-blacklist refuses
     * any world name it cannot validate against this server's current worlds,
     * so an absent one can only be removed once the world exists (or by editing
     * the database directly).
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
                report.warnings.add(String.format(
                        "Blacklisted world '%s' does not exist on this server. It will still be stored, but /remove-from-blacklist will refuse it until that world exists.",
                        lowered));
            }

            if (!dryRun) {
                boolean saved = blacklistDao.save(lowered);
                if (!saved) {
                    report.warnings.add(String.format("Blacklisted world '%s' could not be saved.", lowered));
                    continue;
                }
            }

            // Tracked locally too, so a world repeated in the source file is
            // only ever counted (and written) once per run.
            existing.add(lowered);
            report.blacklistImported++;
        }
    }

    /**
     * Never writes config.yml - issue #40 chose the report-only option over an
     * automatic merge, because a real merge needs the config-merge behaviour
     * from issue #35, which does not exist yet. This only tells the admin what
     * to set and where.
     */
    private void reportConfig(File pluginsDir, ImportReport report) {
        File configFile = new File(pluginsDir, "SetHomes/config.yml");
        if (!configFile.exists()) return;

        YamlConfiguration v1 = YamlConfiguration.loadConfiguration(configFile);

        if (v1.isSet("tp-delay")) {
            report.configNotes.add(String.format("v1 tp-delay: %s -> set delay: %s in config.yml", v1.get("tp-delay"), v1.get("tp-delay")));
        }

        if (v1.isSet("tp-cancelOnMove")) {
            report.configNotes.add(String.format("v1 tp-cancelOnMove: %s -> set cancelOnMove: %s in config.yml", v1.get("tp-cancelOnMove"), v1.get("tp-cancelOnMove")));
        }

        if (v1.isSet("max-homes-msg")) {
            report.configNotes.add(String.format("v1 max-homes-msg: '%s' -> set maxHomesReached: '%s' in config.yml", v1.getString("max-homes-msg"), v1.getString("max-homes-msg")));
        }

        if (v1.isSet("tp-cancelOnMove-msg")) {
            report.configNotes.add(String.format("v1 tp-cancelOnMove-msg: '%s' -> set movedWhileTeleporting: '%s' in config.yml", v1.getString("tp-cancelOnMove-msg"), v1.getString("tp-cancelOnMove-msg")));
        }

        ConfigurationSection maxHomes = v1.getConfigurationSection("max-homes");
        if (maxHomes != null && !maxHomes.getKeys(false).isEmpty()) {
            report.configNotes.add("v1 max-homes -> set maxHomesType: groups and maxHomeEnabled: true in config.yml, then set maxHomes.<group> to:");
            for (String group : maxHomes.getKeys(false)) {
                int limit = maxHomes.getInt(group);
                String value = limit == 0
                        ? "unlimited (v1 treats 0 as unlimited; leave this group out of maxHomes rather than setting it to 0)"
                        : String.valueOf(limit);
                report.configNotes.add(String.format("  maxHomes.%s: %s (v1 max-homes.%s was %d)", group, value, group, limit));
            }
        }

        if (v1.isSet("tp-cooldown")) {
            report.configNotes.add(String.format("v1 tp-cooldown: %s has no Set Homes Two equivalent; teleport cooldown is not supported.", v1.get("tp-cooldown")));
        }
    }
}

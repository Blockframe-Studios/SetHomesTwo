package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        NameLedger ledger = new NameLedger(homesDao);
        Map<UUID, String> cachedNames = HomesImporter.cachedNames();

        // Named homes: allNamedHomes.<uuid>.<name>.{world,x,y,z,pitch,yaw,desc}
        ConfigurationSection allNamed = source.getConfigurationSection("allNamedHomes");
        if (allNamed != null) {
            for (String uuid : allNamed.getKeys(false)) {
                ConfigurationSection playerSection = allNamed.getConfigurationSection(uuid);
                if (playerSection == null) continue;
                for (String homeName : playerSection.getKeys(false)) {
                    importOne(homesDao, ledger, report, playerSection.getConfigurationSection(homeName), uuid, homeName, cachedNames, dryRun);
                }
            }
        }

        // Unnamed default homes: unknownHomes.<uuid>.{world,x,y,z,pitch,yaw}
        ConfigurationSection unknown = source.getConfigurationSection("unknownHomes");
        if (unknown != null) {
            for (String uuid : unknown.getKeys(false)) {
                importOne(homesDao, ledger, report, unknown.getConfigurationSection(uuid), uuid, "default", cachedNames, dryRun);
            }
        }

        importBlacklist(pluginsDir, report, dryRun);
        reportConfig(pluginsDir, report);

        return report;
    }

    private void importOne(HomesDao homesDao, NameLedger ledger, ImportReport report, ConfigurationSection home, String playerUUID, String homeName, Map<UUID, String> cachedNames, boolean dryRun) {
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

            UUID owner = UUID.fromString(playerUUID);

            if (ledger.importedBefore(owner, homeName)) {
                report.skippedExisting++;
                return;
            }

            String storedName = ledger.claim(owner, homeName);
            if (!storedName.equals(homeName)) {
                report.renamed++;
                String note = String.format(
                        "Home '%s' for player %s differs only in capitalization from another of that player's homes, which Set Homes v1 allowed. It takes the name '%s' here, so both locations are kept.",
                        homeName, playerUUID, storedName);
                report.warnings.add(note);
                if (!dryRun) Bukkit.getLogger().warning(note);
            }

            Location location = new Location(
                    world,
                    home.getDouble("x"),
                    home.getDouble("y"),
                    home.getDouble("z"),
                    (float) home.getDouble("yaw"),
                    (float) home.getDouble("pitch")
            );

            String playerName = cachedNames.get(owner);
            if (playerName != null) report.namesResolved++;

            if (!dryRun) {
                Home importedHome = new Home(
                        playerUUID,
                        HomesImporter.defaultMaterial(),
                        location,
                        storedName,
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

    // v1 allowed case-only duplicate names ('base' and 'Base'); v2 does not, so
    // the second one is stored as 'Base2' instead of dropped. Names taken this
    // run are tracked in memory so a dry run reports the same as the confirm.
    private static final class NameLedger {
        private final HomesDao homesDao;
        private final Map<UUID, Set<String>> beforeThisRun = new HashMap<>();
        private final Map<UUID, Set<String>> takenThisRun = new HashMap<>();

        private NameLedger(HomesDao homesDao) {
            this.homesDao = homesDao;
        }

        // True when the player had a home of this name before the run started.
        private boolean importedBefore(UUID player, String name) {
            return namesBeforeThisRun(player).contains(name.toLowerCase());
        }

        // Returns the name asked for, or the first free numbered variant of it.
        private String claim(UUID player, String wanted) {
            String candidate = wanted;

            for (int suffix = 2; isTaken(player, candidate); suffix++) {
                candidate = wanted + suffix;
            }

            takenThisRun.computeIfAbsent(player, p -> new HashSet<>()).add(candidate.toLowerCase());
            return candidate;
        }

        private boolean isTaken(UUID player, String name) {
            String lowered = name.toLowerCase();
            return namesBeforeThisRun(player).contains(lowered)
                    || takenThisRun.getOrDefault(player, Set.of()).contains(lowered);
        }

        private Set<String> namesBeforeThisRun(UUID player) {
            return beforeThisRun.computeIfAbsent(player, p -> {
                Set<String> names = new HashSet<>();
                for (String name : homesDao.namesFor(p)) {
                    names.add(name.toLowerCase());
                }
                return names;
            });
        }
    }

    /**
     * v1's world_blacklist.yml holds a flat blacklisted_worlds list. Missing or
     * empty is normal, not an error. A world absent from this server is still
     * stored, but warned about: /blacklist remove validates against the
     * server's current worlds, so an absent one cannot be removed by command
     * until that world exists.
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
     * Reports the v1 settings that have an equivalent here, and the key to put
     * each one under. Never writes config.yml.
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
            report.configNotes.add(messageNote("max-homes-msg", "maxHomesReached", v1.getString("max-homes-msg")));
        }

        if (v1.isSet("tp-cancelOnMove-msg")) {
            report.configNotes.add(messageNote("tp-cancelOnMove-msg", "movedWhileTeleporting", v1.getString("tp-cancelOnMove-msg")));
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
            report.configNotes.add(String.format("v1 tp-cooldown: %s has no v2 equivalent; teleport cooldown is not supported.", v1.get("tp-cooldown")));
        }
    }

    // A v1 message may carry section-sign color codes, which chat would apply
    // to the rest of the line. Show them as & so the note stays legible.
    private static String messageNote(String v1Key, String v2Key, String value) {
        String shown = value.replace(ChatColor.COLOR_CHAR, '&');
        String note = String.format("v1 %s -> set %s: '%s' in config.yml", v1Key, v2Key, shown);
        if (!shown.equals(value)) {
            note += " (color codes shown as &; copy the original from plugins/SetHomes/config.yml to keep them)";
        }
        return note;
    }
}

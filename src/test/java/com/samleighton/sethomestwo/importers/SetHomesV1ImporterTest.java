package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetHomesV1ImporterTest extends ServerTestBase {

    private final SetHomesV1Importer importer = new SetHomesV1Importer();

    private File setHomesDir() {
        File dir = new File(plugin.getDataFolder().getParentFile(), "SetHomes");
        dir.mkdirs();
        return dir;
    }

    /**
     * SetHomesV1Importer.run() returns early with only a warning when
     * plugins/SetHomes/homes.yml is missing (see the existing code at
     * src/main/java/com/samleighton/sethomestwo/importers/SetHomesV1Importer.java:28-31).
     * Every test that wants the blacklist path to actually run needs this
     * called first, even though these tests have no homes of their own.
     */
    private void writeEmptyHomesFile() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.save(new File(setHomesDir(), "homes.yml"));
    }

    private void writeBlacklist(String... worlds) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blacklisted_worlds", List.of(worlds));
        yaml.save(new File(setHomesDir(), "world_blacklist.yml"));
    }

    @Test
    void dryRunReportsWorldsItWouldBlacklistAndWritesNothing() throws IOException {
        writeEmptyHomesFile();
        writeBlacklist("world_nether");

        ImportReport report = importer.run(true);

        assertEquals(1, report.blacklistImported);
        assertEquals(0, report.blacklistSkippedExisting);
        assertTrue(new BlacklistDao().getAll().isEmpty());
    }

    @Test
    void confirmAddsEachBlacklistedWorldLowercased() throws IOException {
        writeEmptyHomesFile();
        writeBlacklist("World_Nether");

        ImportReport report = importer.run(false);

        assertEquals(1, report.blacklistImported);
        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void reRunningDoesNotDuplicateAnAlreadyBlacklistedWorld() throws IOException {
        writeEmptyHomesFile();
        HomeFixtures.blacklist("world_nether");
        writeBlacklist("world_nether");

        ImportReport report = importer.run(false);

        assertEquals(0, report.blacklistImported);
        assertEquals(1, report.blacklistSkippedExisting);
        assertEquals(1, new BlacklistDao().getAll().size());
    }

    @Test
    void duplicatesWithinTheSourceFileAreOnlyAddedOnce() throws IOException {
        writeEmptyHomesFile();
        writeBlacklist("world_nether", "world_nether");

        ImportReport report = importer.run(false);

        assertEquals(1, report.blacklistImported);
        assertEquals(1, report.blacklistSkippedExisting);
        assertEquals(1, new BlacklistDao().getAll().size());
    }

    @Test
    void aBlacklistedWorldThatDoesNotExistIsStoredWithAWarning() throws IOException {
        writeEmptyHomesFile();
        writeBlacklist("world_the_void");

        ImportReport report = importer.run(false);

        assertEquals(1, report.blacklistImported);
        assertTrue(new BlacklistDao().getAll().contains("world_the_void"));
        assertTrue(report.warnings.stream().anyMatch(w -> w.contains("world_the_void")));
    }

    @Test
    void missingWorldBlacklistFileIsNotAnError() throws IOException {
        // homes.yml present, world_blacklist.yml genuinely absent - the case
        // v1 servers hit constantly, since it shipped empty by default.
        writeEmptyHomesFile();

        ImportReport report = importer.run(true);

        assertEquals(0, report.blacklistImported);
        assertEquals(0, report.blacklistSkippedExisting);
        assertTrue(report.warnings.isEmpty());
    }

    @Test
    void emptyBlacklistedWorldsListIsNotAnError() throws IOException {
        writeEmptyHomesFile();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blacklisted_worlds", List.of());
        yaml.save(new File(setHomesDir(), "world_blacklist.yml"));

        ImportReport report = importer.run(true);

        assertEquals(0, report.blacklistImported);
        assertEquals(0, report.blacklistSkippedExisting);
        assertTrue(report.warnings.isEmpty());
    }
}

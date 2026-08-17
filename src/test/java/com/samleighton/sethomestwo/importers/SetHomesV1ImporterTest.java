package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(report.warnings.stream().anyMatch(w -> w.contains("world_the_void") && w.contains("/remove-from-blacklist")));
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

    @Test
    void aHomeImportedForAPlayerTheServerHasSeenGetsThatPlayersName() throws IOException {
        PlayerMock steve = addPlayer("Steve");
        steve.disconnect();
        writeHomesFile(steve.getUniqueId(), "base", "world");

        importer.run(false);

        assertEquals("Steve", new HomesDao().get(steve.getUniqueId(), "base").getPlayerName());
    }

    @Test
    void aHomeImportedForAPlayerTheServerHasNeverSeenGetsNoNameAndNoWarning() throws IOException {
        UUID neverSeen = UUID.randomUUID();
        writeHomesFile(neverSeen, "base", "world");

        ImportReport report = importer.run(false);

        assertNull(new HomesDao().get(neverSeen, "base").getPlayerName());
        assertTrue(report.warnings.isEmpty());
    }

    @Test
    void dryRunReportsNamesResolvedWithoutWritingAnything() throws IOException {
        PlayerMock steve = addPlayer("Steve");
        steve.disconnect();
        writeHomesFile(steve.getUniqueId(), "base", "world");

        ImportReport report = importer.run(true);

        assertEquals(1, report.namesResolved);
        assertNull(new HomesDao().get(steve.getUniqueId(), "base"));
    }

    private void writeHomesFile(UUID owner, String homeName, String worldName) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        String path = "allNamedHomes." + owner + "." + homeName + ".";
        yaml.set(path + "world", worldName);
        yaml.set(path + "x", 0.0);
        yaml.set(path + "y", 64.0);
        yaml.set(path + "z", 0.0);
        yaml.set(path + "pitch", 0.0);
        yaml.set(path + "yaw", 0.0);
        yaml.save(new File(setHomesDir(), "homes.yml"));
    }

    @Test
    void directlyMappedSettingsAreReportedWithBothKeys() throws IOException {
        writeEmptyHomesFile();
        writeV1Config(v1 -> {
            v1.set("tp-delay", 5);
            v1.set("tp-cancelOnMove", true);
        });

        ImportReport report = importer.run(true);

        assertTrue(report.configNotes.stream().anyMatch(n -> n.contains("tp-delay") && n.contains("delay")));
        assertTrue(report.configNotes.stream().anyMatch(n -> n.contains("tp-cancelOnMove") && n.contains("cancelOnMove")));
    }

    @Test
    void tpCooldownIsCalledOutAsHavingNoEquivalent() throws IOException {
        writeEmptyHomesFile();
        writeV1Config(v1 -> v1.set("tp-cooldown", 30));

        ImportReport report = importer.run(true);

        assertTrue(report.configNotes.stream().anyMatch(n -> n.contains("tp-cooldown") && n.contains("no Set Homes Two equivalent")));
    }

    @Test
    void aZeroMaxHomesGroupIsDescribedAsUnlimitedNotZero() throws IOException {
        writeEmptyHomesFile();
        writeV1Config(v1 -> v1.set("max-homes.default", 0));

        ImportReport report = importer.run(true);

        String note = report.configNotes.stream()
                .filter(n -> n.contains("default"))
                .findFirst()
                .orElseThrow();
        assertTrue(note.contains("unlimited"));
        assertFalse(note.matches(".*\\bmaxHomes\\.default:\\s*0\\b.*"));
    }

    @Test
    void aNonZeroMaxHomesGroupIsReportedWithItsNumber() throws IOException {
        writeEmptyHomesFile();
        writeV1Config(v1 -> v1.set("max-homes.vip", 6));

        ImportReport report = importer.run(true);

        assertTrue(report.configNotes.stream().anyMatch(n -> n.contains("vip") && n.contains("6")));
    }

    @Test
    void missingV1ConfigFileProducesNoNotes() throws IOException {
        writeEmptyHomesFile();

        ImportReport report = importer.run(true);

        assertTrue(report.configNotes.isEmpty());
    }

    @Test
    void theConfigReportNeverWritesToConfigYml() throws IOException {
        writeEmptyHomesFile();
        writeV1Config(v1 -> v1.set("tp-delay", 5));
        long before = new File(plugin.getDataFolder(), "config.yml").lastModified();

        importer.run(false);

        assertEquals(before, new File(plugin.getDataFolder(), "config.yml").lastModified());
    }

    // Case-only duplicates from v1: 'base' and 'Base' were two homes there.

    private final YamlConfiguration v1Homes = new YamlConfiguration();

    // Adds one entry to allNamedHomes. Distinct x values tell the rows apart.
    private void addV1Home(UUID owner, String homeName, String worldName, double x) {
        String path = "allNamedHomes." + owner + "." + homeName + ".";
        v1Homes.set(path + "world", worldName);
        v1Homes.set(path + "x", x);
        v1Homes.set(path + "y", 64.0);
        v1Homes.set(path + "z", 0.0);
        v1Homes.set(path + "pitch", 0.0);
        v1Homes.set(path + "yaw", 0.0);
    }

    // Adds the player's unnamed v1 home, which imports as 'default'.
    private void addV1UnnamedHome(UUID owner, double x) {
        String path = "unknownHomes." + owner + ".";
        v1Homes.set(path + "world", "world");
        v1Homes.set(path + "x", x);
        v1Homes.set(path + "y", 64.0);
        v1Homes.set(path + "z", 0.0);
        v1Homes.set(path + "pitch", 0.0);
        v1Homes.set(path + "yaw", 0.0);
    }

    private void saveV1Homes() throws IOException {
        v1Homes.save(new File(setHomesDir(), "homes.yml"));
    }

    private String counts(ImportReport report) {
        return String.format("imported=%d renamed=%d skippedExisting=%d skippedWorldMissing=%d failed=%d namesResolved=%d",
                report.imported, report.renamed, report.skippedExisting,
                report.skippedWorldMissing, report.failed, report.namesResolved);
    }

    private List<String> homeNamesOf(UUID owner) {
        return new HomesDao().getAll(owner).stream().map(Home::getName).toList();
    }

    @Test
    void aCaseOnlyDuplicateIsImportedUnderAFreeNameRatherThanDropped() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();

        ImportReport report = importer.run(false);

        assertEquals(2, report.imported);
        assertEquals(1, report.renamed);
        assertEquals(0, report.skippedExisting);
        assertEquals(List.of("base", "Base2"), homeNamesOf(owner));
    }

    @Test
    void theRenamedHomeKeepsItsOwnLocation() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();

        importer.run(false);

        HomesDao dao = new HomesDao();
        assertEquals(10.0, dao.get(owner, "base").getX());
        assertEquals(20.0, dao.get(owner, "Base2").getX());
    }

    @Test
    void theRenameIsReportedWithBothTheOldAndTheNewName() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();

        ImportReport report = importer.run(false);

        assertTrue(report.warnings.stream().anyMatch(w -> w.contains("'Base'") && w.contains("'Base2'")),
                "no warning naming the old and new name, got: " + report.warnings);
    }

    @Test
    void theRenameIsRecordedInTheServerLog() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();

        List<String> logged = new ArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { logged.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Bukkit.getLogger().addHandler(capture);
        try {
            importer.run(false);
        } finally {
            Bukkit.getLogger().removeHandler(capture);
        }

        assertTrue(logged.stream().anyMatch(m -> m.contains("'Base'") && m.contains("'Base2'")),
                "the rename was not logged, got: " + logged);
    }

    @Test
    void aGenuineReImportRenamesNothingAndImportsNothing() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();
        importer.run(false);

        ImportReport second = importer.run(false);

        assertEquals(0, second.imported);
        assertEquals(0, second.renamed);
        assertEquals(2, second.skippedExisting);
        assertEquals(2, new HomesDao().getAll(owner).size());
    }

    @Test
    void theDryRunReportsTheSameNumbersAsTheConfirmThatFollowsIt() throws IOException {
        PlayerMock steve = addPlayer("Steve");
        steve.disconnect();
        addV1Home(steve.getUniqueId(), "base", "world", 10.0);
        addV1Home(steve.getUniqueId(), "Base", "world", 20.0);
        addV1Home(steve.getUniqueId(), "gone", "world_deleted", 30.0);
        saveV1Homes();

        ImportReport dryRun = importer.run(true);
        ImportReport confirm = importer.run(false);

        assertEquals(counts(dryRun), counts(confirm));
    }

    @Test
    void aDryRunDetectsACaseOnlyDuplicateWithoutWritingAnything() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        saveV1Homes();

        ImportReport report = importer.run(true);

        assertEquals(1, report.renamed);
        assertTrue(new HomesDao().getAll(owner).isEmpty());
    }

    @Test
    void noHomeIsLostWhenTheDisambiguatedNameIsItselfTakenLaterInTheFile() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "base", "world", 10.0);
        addV1Home(owner, "Base", "world", 20.0);
        addV1Home(owner, "base2", "world", 30.0);
        saveV1Homes();

        importer.run(false);

        List<Double> imported = new HomesDao().getAll(owner).stream().map(Home::getX).sorted().toList();
        assertEquals(List.of(10.0, 20.0, 30.0), imported);
        List<String> lowered = homeNamesOf(owner).stream().map(String::toLowerCase).distinct().toList();
        assertEquals(3, lowered.size(), "names must stay unique ignoring case, got: " + homeNamesOf(owner));
    }

    @Test
    void anUnnamedHomeSurvivesAPlayerAlreadyHavingAHomeCalledDefault() throws IOException {
        UUID owner = UUID.randomUUID();
        addV1Home(owner, "default", "world", 10.0);
        addV1UnnamedHome(owner, 20.0);
        saveV1Homes();

        ImportReport report = importer.run(false);

        assertEquals(2, report.imported);
        assertEquals(1, report.renamed);
        assertEquals(2, new HomesDao().getAll(owner).size());
    }

    @Test
    void theFreeNameSearchKeepsGoingPastANameAlreadyInTheDatabase() throws IOException {
        PlayerMock owner = addPlayer();
        HomeFixtures.persist(owner, "Base2");
        addV1Home(owner.getUniqueId(), "base", "world", 10.0);
        addV1Home(owner.getUniqueId(), "Base", "world", 20.0);
        saveV1Homes();

        importer.run(false);

        assertEquals(List.of("Base2", "base", "Base3"), homeNamesOf(owner.getUniqueId()));
    }

    @Test
    void bothHomesOfACaseOnlyPairAreReachableByCommand() throws IOException {
        TestPlayer traveller = addTestPlayer("traveller");
        plugin.getConfig().set("teleportSafety", false);
        plugin.getConfig().set("delay", 0);
        addV1Home(traveller.getUniqueId(), "base", "world", 10.0);
        addV1Home(traveller.getUniqueId(), "Base", "world", 20.0);
        saveV1Homes();
        importer.run(false);

        server.execute("go-home", traveller, "base").assertSucceeded();
        server.getScheduler().performTicks(100L);
        assertEquals(10, traveller.getLocation().getBlockX());

        server.execute("go-home", traveller, "Base2").assertSucceeded();
        server.getScheduler().performTicks(100L);
        assertEquals(20, traveller.getLocation().getBlockX());
    }

    private void writeV1Config(java.util.function.Consumer<YamlConfiguration> body) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        body.accept(yaml);
        yaml.save(new File(setHomesDir(), "config.yml"));
    }
}

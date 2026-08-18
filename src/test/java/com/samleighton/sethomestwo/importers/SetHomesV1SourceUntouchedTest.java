package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The migration is reversible only because v2 never writes, renames or deletes
 * anything under plugins/SetHomes/. Put the old jar back and the server is as
 * it was. Nothing else in the suite holds the importer to that.
 */
class SetHomesV1SourceUntouchedTest extends ServerTestBase {

    private final SetHomesV1Importer importer = new SetHomesV1Importer();

    private File v1Dir;

    @BeforeEach
    void writeV1Source() throws IOException {
        v1Dir = new File(plugin.getDataFolder().getParentFile(), "SetHomes");
        if (!v1Dir.isDirectory() && !v1Dir.mkdirs())
            throw new IllegalStateException("could not create " + v1Dir);

        writeHomes();
        writeBlacklist();
        writeConfig();
    }

    @Test
    void aDryRunLeavesEveryV1FileExactlyAsItWas() throws IOException {
        Map<String, String> before = fingerprint(v1Dir);

        importer.run(true);

        assertEquals(before, fingerprint(v1Dir),
                "a preview that writes to v1's folder is not a preview");
    }

    @Test
    void aConfirmedImportLeavesEveryV1FileExactlyAsItWas() throws IOException {
        Map<String, String> before = fingerprint(v1Dir);

        ImportReport report = importer.run(false);

        // Without this the test would also pass for an importer that did nothing.
        assertTrue(report.imported > 0, "the import should have brought homes across");
        assertTrue(report.renamed > 0, "the fixture holds a case-only duplicate");
        assertTrue(report.skippedWorldMissing > 0, "the fixture holds a home in a missing world");

        assertEquals(before, fingerprint(v1Dir),
                "rolling back to v1 depends on its files being untouched");
    }

    /**
     * Path to SHA-256 for every file under the directory. Comparing the whole
     * map catches an edited file, a new one and a deleted one alike.
     */
    private Map<String, String> fingerprint(File dir) throws IOException {
        Map<String, String> digests = new TreeMap<>();
        File[] entries = dir.listFiles();
        if (entries == null) throw new IllegalStateException("not a directory: " + dir);

        for (File entry : entries) {
            if (entry.isDirectory()) {
                // Recorded in its own right, or an added empty directory would
                // contribute no entries and slip through unnoticed.
                digests.put(entry.getName() + "/", "directory");
                fingerprint(entry).forEach((path, digest) -> digests.put(entry.getName() + "/" + path, digest));
            } else {
                digests.put(entry.getName(), sha256(entry));
            }
        }
        return digests;
    }

    private String sha256(File file) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every JVM", e);
        }
    }

    /**
     * The cases that make the importer work hardest, so the test is not proving
     * read-only behavior on a file the importer barely looks at: a home in a
     * world that no longer exists, a case-only duplicate name, an unnamed home,
     * and a player the server has never seen.
     */
    private void writeHomes() throws IOException {
        String owner = UUID.randomUUID().toString();
        String stranger = UUID.randomUUID().toString();

        YamlConfiguration yaml = new YamlConfiguration();
        home(yaml, "allNamedHomes." + owner + ".base", "world");
        home(yaml, "allNamedHomes." + owner + ".Base", "world");
        home(yaml, "allNamedHomes." + owner + ".nether", "world_nether");
        home(yaml, "allNamedHomes." + owner + ".plotworld", "creative");
        home(yaml, "unknownHomes." + stranger, "world");
        yaml.save(new File(v1Dir, "homes.yml"));
    }

    private void home(YamlConfiguration yaml, String path, String world) {
        yaml.set(path + ".world", world);
        yaml.set(path + ".x", 1.5);
        yaml.set(path + ".y", 64.0);
        yaml.set(path + ".z", -2.5);
        yaml.set(path + ".pitch", 12.0);
        yaml.set(path + ".yaw", 45.0);
    }

    private void writeBlacklist() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blacklisted_worlds", List.of("world_the_end"));
        yaml.save(new File(v1Dir, "world_blacklist.yml"));
    }

    private void writeConfig() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("tp-delay", 5);
        yaml.set("tp-cancelOnMove", true);
        yaml.set("max-homes.default", 3);
        yaml.save(new File(v1Dir, "config.yml"));
    }
}

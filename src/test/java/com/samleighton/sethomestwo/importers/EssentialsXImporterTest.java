package com.samleighton.sethomestwo.importers;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentialsXImporterTest extends ServerTestBase {

    private final EssentialsXImporter importer = new EssentialsXImporter();

    private File userdataDir() {
        File dir = new File(plugin.getDataFolder().getParentFile(), "Essentials/userdata");
        dir.mkdirs();
        return dir;
    }

    private void writeUserFile(UUID owner, String lastAccountName, String homeName, String worldName) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        if (lastAccountName != null) {
            yaml.set("lastAccountName", lastAccountName);
        }
        String path = "homes." + homeName + ".";
        yaml.set(path + "world", worldName);
        yaml.set(path + "x", 0.0);
        yaml.set(path + "y", 64.0);
        yaml.set(path + "z", 0.0);
        yaml.set(path + "pitch", 0.0);
        yaml.set(path + "yaw", 0.0);
        yaml.save(new File(userdataDir(), owner + ".yml"));
    }

    @Test
    void theImportedHomeGetsTheStoredAccountName() throws IOException {
        UUID owner = UUID.randomUUID();
        writeUserFile(owner, "Steve", "base", "world");

        ImportReport report = importer.run(false);

        assertEquals("Steve", new HomesDao().get(owner, "base").getPlayerName());
        assertEquals(1, report.namesResolved);
    }

    @Test
    void aUserFileWithNoLastAccountNameImportsWithNoNameAndNoWarning() throws IOException {
        UUID owner = UUID.randomUUID();
        writeUserFile(owner, null, "base", "world");

        ImportReport report = importer.run(false);

        assertNull(new HomesDao().get(owner, "base").getPlayerName());
        assertTrue(report.warnings.isEmpty());
    }
}

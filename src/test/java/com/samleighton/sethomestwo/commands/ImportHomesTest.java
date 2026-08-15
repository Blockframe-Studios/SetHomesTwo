package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportHomesTest extends ServerTestBase {

    private File setHomesDir() {
        File dir = new File(plugin.getDataFolder().getParentFile(), "SetHomes");
        dir.mkdirs();
        return dir;
    }

    private void writeEmptyHomesFile() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.save(new File(setHomesDir(), "homes.yml"));
    }

    private void writeBlacklist(String... worlds) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blacklisted_worlds", List.of(worlds));
        yaml.save(new File(setHomesDir(), "world_blacklist.yml"));
    }

    private PlayerMock authorizedPlayer() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.import-homes", true);
        return player;
    }

    @Test
    void blacklistActivityAddsASecondReplyLine() throws IOException {
        writeEmptyHomesFile();
        writeBlacklist("world_nether");
        PlayerMock player = authorizedPlayer();

        server.execute("import-homes", player, "sethomes").assertSucceeded();

        player.nextMessage(); // homes summary line
        assertTrue(player.nextMessage().contains("blacklist"));
    }

    @Test
    void noBlacklistActivityMeansNoSecondLine() throws IOException {
        writeEmptyHomesFile();
        PlayerMock player = authorizedPlayer();

        server.execute("import-homes", player, "sethomes").assertSucceeded();

        // 0 homes and 0 blacklist activity: only the summary line is sent.
        // The dry-run hint is gated on (imported > 0 || hasBlacklistActivity()),
        // so it does not fire here either.
        String summary = player.nextMessage();
        assertFalse(summary.contains("blacklist"));
        assertNull(player.nextMessage());
    }

    @Test
    void configNotesArePrintedAsConfigLines() throws IOException {
        writeEmptyHomesFile();
        YamlConfiguration v1Config = new YamlConfiguration();
        v1Config.set("tp-cooldown", 30);
        v1Config.save(new File(setHomesDir(), "config.yml"));
        PlayerMock player = authorizedPlayer();

        server.execute("import-homes", player, "sethomes").assertSucceeded();

        boolean sawConfigLine = false;
        String message;
        while ((message = player.nextMessage()) != null) {
            if (message.startsWith("Config: ") && message.contains("tp-cooldown")) {
                sawConfigLine = true;
            }
        }
        assertTrue(sawConfigLine);
    }
}

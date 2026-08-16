package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetMaxHomesTest extends ServerTestBase {

    // SetMaxHomes reads and writes config.yml directly from disk rather than
    // through plugin.getConfig(), so tests that need a particular maxHomesType
    // write the file themselves.
    private void useSingularMaxHomes() throws IOException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("maxHomesType", "singular");
        config.set("maxHomes", 5);
        config.save(configFile);
    }

    @Test
    void theAliasWorks() throws IOException {
        useSingularMaxHomes();
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.set-max-homes", true);

        server.execute("setmax", player, "7");

        assertEquals(7, savedConfig().getInt("maxHomes"));
    }

    @Test
    void theUsageNamesTheAliasThatWasTyped() throws IOException {
        useSingularMaxHomes();
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.set-max-homes", true);

        server.dispatchCommand(player, "setmax");

        player.nextMessage();
        assertTrue(player.nextMessage().contains("Usage: /setmax"));
    }

    // The shipped default-config.yml is in groups mode, where maxHomes is a
    // section (admin: 5, user: 4) rather than a scalar. getString("maxHomes")
    // returns null for a section, which used to be misread as "not configured".
    @Test
    void groupsModeAcceptsTheShippedDefaultConfig() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.set-max-homes", true);

        server.execute("set-max-homes", player, "admin", "9");

        assertEquals(9, savedConfig().getInt("maxHomes.admin"));
        assertEquals(4, savedConfig().getInt("maxHomes.user"));
    }

    @Test
    void groupsModeRejectsAnUnknownGroup() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.set-max-homes", true);

        server.execute("set-max-homes", player, "nope", "9");

        assertTrue(player.nextMessage().contains("Group does not exist"));
    }

    private YamlConfiguration savedConfig() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }
}

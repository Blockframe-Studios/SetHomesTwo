package com.samleighton.sethomestwo;

import com.samleighton.sethomestwo.support.FailOnUnimplemented;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.PluginBoot;
import com.samleighton.sethomestwo.support.TestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the plugin by hand rather than through ServerTestBase, because v1's
 * homes.yml has to be on disk before our onEnable looks for it.
 */
@ExtendWith(FailOnUnimplemented.class)
class PendingV1ImportNoticeTest {

    private ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();

        // Same overworld, nether, end order ServerTestBase uses: ServerUtil maps
        // environments onto worlds by list position.
        server.addSimpleWorld("world").setEnvironment(World.Environment.NORMAL);
        server.addSimpleWorld("world_nether").setEnvironment(World.Environment.NETHER);
        server.addSimpleWorld("world_the_end").setEnvironment(World.Environment.THE_END);
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void saysSoAtStartupWhenV1HomesAreWaitingAndNothingIsImported() {
        writeV1Homes(2, 0);

        String block = warningText(captureLog(PluginBoot::load));

        assertTrue(block.contains("plugins/SetHomes/homes.yml"), "should name the file it found");
        assertTrue(block.contains("2"), "should say how many homes are waiting");
        assertTrue(block.contains("/import-homes sethomes"), "should name the command to run");
    }

    @Test
    void v1UnnamedHomesAreCountedToo() {
        writeV1Homes(1, 1);

        String block = warningText(captureLog(PluginBoot::load));

        assertTrue(block.contains("2 home"),
                "v1 keeps a player's unnamed home under unknownHomes, and it imports like any other");
    }

    @Test
    void aJoiningAdminIsToldTheImportIsWaiting() {
        writeV1Homes(2, 0);
        SetHomesTwo plugin = PluginBoot.load();

        TestPlayer admin = join(plugin, "Admin", true);

        assertTrue(messagesTo(admin).contains("/import-homes sethomes"),
                "plenty of admins never read the console, so the notice has to reach them in chat");
    }

    @Test
    void aJoiningPlayerWithoutTheImportPermissionIsNotTold() {
        writeV1Homes(2, 0);
        SetHomesTwo plugin = PluginBoot.load();

        TestPlayer player = join(plugin, "Regular", false);

        assertFalse(messagesTo(player).contains("import-homes"),
                "only someone who can run the import has any use for the notice");
    }

    @Test
    void theNoticeRepeatsOnEveryJoin() {
        writeV1Homes(2, 0);
        SetHomesTwo plugin = PluginBoot.load();
        TestPlayer admin = join(plugin, "Admin", true);
        messagesTo(admin);
        admin.disconnect();

        // MockBukkit drops the attachment on disconnect; a real op or permissions
        // group survives a reconnect, so grant it again before the second join.
        admin.addAttachment(plugin, "sh2.import-homes", true);
        server.addPlayer(admin);

        assertTrue(messagesTo(admin).contains("/import-homes sethomes"),
                "nothing is remembered per player, so an admin who missed it sees it next time");
    }

    @Test
    void theNoticeStopsOnceAHomeExistsHere() {
        writeV1Homes(2, 0);
        SetHomesTwo plugin = PluginBoot.load();
        TestPlayer first = join(plugin, "First", true);
        HomeFixtures.persist(first, "base");

        TestPlayer second = join(plugin, "Second", true);

        assertFalse(messagesTo(second).contains("import-homes"),
                "an empty database is the whole condition, so one home ends the notice");
    }

    @Test
    void nothingIsSaidWhenThereIsNoV1File() {
        String block = warningText(captureLog(PluginBoot::load));

        assertFalse(block.contains("import-homes"), "most servers have never had v1 installed");
    }

    @Test
    void nothingIsSaidWhenTheV1FileHoldsNoHomes() {
        writeV1Homes(0, 0);

        String block = warningText(captureLog(PluginBoot::load));

        assertFalse(block.contains("import-homes"), "an empty homes.yml has nothing to offer");
    }

    /**
     * Joins a player, granting sh2.import-homes outright rather than opping, so
     * the tests pin the permission the notice is actually gated on.
     */
    private TestPlayer join(SetHomesTwo plugin, String name, boolean mayImport) {
        TestPlayer player = new TestPlayer(server, name);
        if (mayImport) player.addAttachment(plugin, "sh2.import-homes", true);
        server.addPlayer(player);
        return player;
    }

    private String messagesTo(TestPlayer player) {
        StringBuilder all = new StringBuilder();
        String message;
        while ((message = player.nextMessage()) != null) all.append(message).append("\n");
        return all.toString();
    }

    /**
     * Writes a v1 homes.yml in the layout SetHomesV1Importer reads: named homes
     * under allNamedHomes.uuid.name, unnamed ones under unknownHomes.uuid.
     */
    private void writeV1Homes(int named, int unnamed) {
        YamlConfiguration source = new YamlConfiguration();
        for (int i = 0; i < named; i++) {
            writeOne(source, "allNamedHomes." + UUID.randomUUID() + ".home" + i);
        }
        for (int i = 0; i < unnamed; i++) {
            writeOne(source, "unknownHomes." + UUID.randomUUID());
        }

        File v1Dir = new File(server.getPluginsFolder(), "SetHomes");
        if (!v1Dir.isDirectory() && !v1Dir.mkdirs())
            throw new IllegalStateException("could not create " + v1Dir);
        try {
            source.save(new File(v1Dir, "homes.yml"));
        } catch (IOException e) {
            throw new IllegalStateException("could not write the v1 fixture", e);
        }
    }

    private void writeOne(YamlConfiguration source, String path) {
        source.set(path + ".world", "world");
        source.set(path + ".x", 1.0);
        source.set(path + ".y", 64.0);
        source.set(path + ".z", 1.0);
        source.set(path + ".pitch", 0.0);
        source.set(path + ".yaw", 0.0);
    }

    private List<LogRecord> captureLog(Runnable action) {
        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        Logger logger = Bukkit.getLogger();
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return captured;
    }

    private String warningText(List<LogRecord> records) {
        return records.stream()
                .filter(record -> record.getLevel() == Level.WARNING)
                .map(LogRecord::getMessage)
                .collect(Collectors.joining("\n"));
    }
}

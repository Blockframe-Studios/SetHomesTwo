package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionOverridesTest extends ServerTestBase {

    @Test
    void aConfiguredDefaultIsApplied() {
        plugin.getConfig().set("permissions.sh2.import-homes", "true");

        PermissionOverrides.apply();

        assertEquals(PermissionDefault.TRUE,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
    }

    @Test
    void notOpIsAccepted() {
        plugin.getConfig().set("permissions.sh2.manage-homes", "not-op");

        PermissionOverrides.apply();

        assertEquals(PermissionDefault.NOT_OP,
                server.getPluginManager().getPermission("sh2.manage-homes").getDefault());
    }

    @Test
    void anUnknownNodeIsIgnored() {
        plugin.getConfig().set("permissions.sh2.not-a-real-node", "true");

        List<LogRecord> logged = captureLog(PermissionOverrides::apply);

        assertEquals(null, server.getPluginManager().getPermission("sh2.not-a-real-node"));
        assertTrue(loggedWarning(logged,
                        "SetHomesTwo: ignoring unknown permission node 'sh2.not-a-real-node' in config.yml."),
                "Expected a warning naming the unknown node");
    }

    @Test
    void anUnparseableValueLeavesTheDefaultAlone() {
        plugin.getConfig().set("permissions.sh2.import-homes", "sometimes");

        List<LogRecord> logged = captureLog(PermissionOverrides::apply);

        assertEquals(PermissionDefault.OP,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
        assertTrue(loggedWarning(logged,
                        "SetHomesTwo: ignoring permission 'sh2.import-homes', value 'sometimes' is not one of true, false, op, not-op."),
                "Expected a warning naming the node and the rejected value");
    }

    @Test
    void aWildcardIsNotHonoured() {
        plugin.getConfig().set("permissions.sh2.*", "true");

        PermissionOverrides.apply();

        assertEquals(PermissionDefault.OP,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
    }

    @Test
    void noPermissionsSectionLeavesStockDefaults() {
        PermissionOverrides.apply();

        assertEquals(PermissionDefault.OP,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
        assertEquals(PermissionDefault.TRUE,
                server.getPluginManager().getPermission("sh2.create-home").getDefault());
    }

    @Test
    void anExplicitGrantStillBeatsAConfiguredDefault() {
        plugin.getConfig().set("permissions.sh2.import-homes", "false");
        PermissionOverrides.apply();

        var player = addPlayer();
        player.addAttachment(plugin, "sh2.import-homes", true);

        org.junit.jupiter.api.Assertions.assertTrue(player.hasPermission("sh2.import-homes"));
    }

    /**
     * Captures what gets logged during {@code action}. The handler is always
     * removed afterward so it cannot leak into other tests.
     */
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

    private boolean loggedWarning(List<LogRecord> records, String message) {
        return records.stream().anyMatch(
                record -> record.getLevel() == Level.WARNING && message.equals(record.getMessage()));
    }
}

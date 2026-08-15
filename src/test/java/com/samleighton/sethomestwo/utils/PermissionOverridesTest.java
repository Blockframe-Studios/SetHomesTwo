package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertEquals(PermissionDefault.FALSE,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
        assertTrue(opped().hasPermission("sh2.import-homes"));
        assertTrue(loggedWarning(logged,
                        "SetHomesTwo: ignoring permission 'sh2.import-homes', value 'sometimes' is not one of true, false, op, not-op."),
                "Expected a warning naming the node and the rejected value");
    }

    @Test
    void aWildcardIsNotHonoured() {
        plugin.getConfig().set("permissions.sh2.*", "true");

        PermissionOverrides.apply();

        assertEquals(PermissionDefault.FALSE,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
        assertTrue(opped().hasPermission("sh2.import-homes"));
    }

    @Test
    void noPermissionsSectionLeavesStockDefaults() {
        PermissionOverrides.apply();

        assertEquals(PermissionDefault.FALSE,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
        assertTrue(opped().hasPermission("sh2.import-homes"));
        // The player nodes declare false and are granted by the sh2.player
        // bundle, so denying the bundle takes all eight away at once.
        assertEquals(PermissionDefault.FALSE,
                server.getPluginManager().getPermission("sh2.create-home").getDefault());
        assertTrue(addPlayer().hasPermission("sh2.create-home"));
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

    @Test
    void aDeniedNodeIsDeniedDespiteItsBundle() {
        plugin.getConfig().set("permissions.sh2.manage-homes", false);

        PermissionOverrides.apply();

        // sh2.player is default true and lists sh2.manage-homes as a child, so
        // Bukkit writes the child straight into every player's effective map.
        // Lowering the node's own default is not enough on its own.
        assertFalse(addPlayer().hasPermission("sh2.manage-homes"));
    }

    @Test
    void aDeniedAdminNodeIsDeniedForOperators() {
        plugin.getConfig().set("permissions.sh2.import-homes", false);

        PermissionOverrides.apply();

        PlayerMock op = addPlayer();
        op.setOp(true);

        assertFalse(op.hasPermission("sh2.import-homes"));
    }

    @Test
    void aNodeRaisedToOpNoLongerReachesOrdinaryPlayers() {
        plugin.getConfig().set("permissions.sh2.manage-homes", "op");

        PermissionOverrides.apply();

        PlayerMock player = addPlayer();
        PlayerMock op = addPlayer("Op");
        op.setOp(true);

        assertFalse(player.hasPermission("sh2.manage-homes"));
        assertTrue(op.hasPermission("sh2.manage-homes"));
    }

    @Test
    void aGrantedNodeStillReachesPlayersThroughItsBundle() {
        plugin.getConfig().set("permissions.sh2.get-player-homes", true);

        PermissionOverrides.apply();

        assertTrue(addPlayer().hasPermission("sh2.get-player-homes"));
        // Untouched nodes must keep working.
        assertTrue(addPlayer("Other").hasPermission("sh2.create-home"));
    }

    @Test
    void denyingTheBundleTakesEveryPlayerNodeAtOnce() {
        plugin.getConfig().set("permissions.sh2.player", false);

        PermissionOverrides.apply();

        PlayerMock player = addPlayer();
        assertFalse(player.hasPermission("sh2.player"));
        assertFalse(player.hasPermission("sh2.create-home"));
        assertFalse(player.hasPermission("sh2.go-home"));
        assertFalse(player.hasPermission("sh2.manage-homes"));
    }

    @Test
    void denyingTheAdminBundleLeavesOrdinaryPlayersAlone() {
        plugin.getConfig().set("permissions.sh2.admin", false);

        PermissionOverrides.apply();

        PlayerMock op = addPlayer();
        op.setOp(true);

        assertFalse(op.hasPermission("sh2.import-homes"));
        assertTrue(addPlayer("Plain").hasPermission("sh2.create-home"));
    }

    /**
     * The admin nodes declare false and are granted by the sh2.admin bundle,
     * which is op by default, so effective access is what tells you whether a
     * node is still operator only.
     */
    private PlayerMock opped() {
        PlayerMock op = addPlayer("Op" + (opCount++));
        op.setOp(true);
        return op;
    }

    private int opCount = 0;
}

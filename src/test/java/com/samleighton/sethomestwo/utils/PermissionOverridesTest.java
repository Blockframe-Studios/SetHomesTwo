package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        PermissionOverrides.apply();

        assertEquals(null, server.getPluginManager().getPermission("sh2.not-a-real-node"));
    }

    @Test
    void anUnparseableValueLeavesTheDefaultAlone() {
        plugin.getConfig().set("permissions.sh2.import-homes", "sometimes");

        PermissionOverrides.apply();

        assertEquals(PermissionDefault.OP,
                server.getPluginManager().getPermission("sh2.import-homes").getDefault());
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
}

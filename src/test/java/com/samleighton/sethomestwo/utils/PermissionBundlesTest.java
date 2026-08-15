package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionBundlesTest extends ServerTestBase {

    @Test
    void theAdminBundleConfersItsChildren() {
        assertNotNull(server.getPluginManager().getPermission("sh2.admin"));

        var player = addPlayer();
        player.addAttachment(plugin, "sh2.admin", true);

        assertTrue(player.hasPermission("sh2.get-player-homes"));
        assertTrue(player.hasPermission("sh2.set-max-homes"));
        assertTrue(player.hasPermission("sh2.import-homes"));
    }

    @Test
    void thePlayerBundleConfersItsChildren() {
        var player = addPlayer();
        player.addAttachment(plugin, "sh2.player", true);

        assertTrue(player.hasPermission("sh2.create-home"));
        assertTrue(player.hasPermission("sh2.go-home"));
        assertTrue(player.hasPermission("sh2.manage-homes"));
    }

    @Test
    void theBundleDefaultsMatchTheirMembers() {
        assertEquals(PermissionDefault.TRUE,
                server.getPluginManager().getPermission("sh2.player").getDefault());
        assertEquals(PermissionDefault.OP,
                server.getPluginManager().getPermission("sh2.admin").getDefault());
    }
}

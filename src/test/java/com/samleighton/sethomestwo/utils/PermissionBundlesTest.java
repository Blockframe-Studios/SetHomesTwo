package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionBundlesTest extends ServerTestBase {

    @Test
    void theAdminBundleConfersItsChildren() {
        Permission adminBundle = server.getPluginManager().getPermission("sh2.admin");
        assertNotNull(adminBundle);
        assertTrue(adminBundle.getChildren().containsKey("sh2.player"));
        assertTrue(adminBundle.getChildren().containsKey("sh2.get-player-homes"));
        assertTrue(adminBundle.getChildren().containsKey("sh2.set-max-homes"));
        assertTrue(adminBundle.getChildren().containsKey("sh2.import-homes"));

        var player = addPlayer();
        player.addAttachment(plugin, "sh2.admin", true);

        assertTrue(player.hasPermission("sh2.get-player-homes"));
        assertTrue(player.hasPermission("sh2.set-max-homes"));
        assertTrue(player.hasPermission("sh2.import-homes"));
    }

    @Test
    void thePlayerBundleConfersItsChildren() {
        Permission playerBundle = server.getPluginManager().getPermission("sh2.player");
        assertNotNull(playerBundle);
        assertTrue(playerBundle.getChildren().containsKey("sh2.create-home"));
        assertTrue(playerBundle.getChildren().containsKey("sh2.go-home"));
        assertTrue(playerBundle.getChildren().containsKey("sh2.manage-homes"));

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

package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void stockDefaultsGiveOrdinaryPlayersTheirNodesAndNothingElse() {
        PlayerMock player = addPlayer();

        for (String node : new String[]{"sh2.create-home", "sh2.go-home", "sh2.list-homes",
                "sh2.delete-home", "sh2.teleport", "sh2.give-homes-item", "sh2.manage-homes",
                "sh2.move-home"}) {
            assertTrue(player.hasPermission(node), node);
        }

        for (String node : new String[]{"sh2.import-homes", "sh2.get-player-homes",
                "sh2.set-max-homes", "sh2.add-to-blacklist", "sh2.remove-from-blacklist",
                "sh2.get-blacklisted-dimensions", "sh2.go-player-home", "sh2.delete-player-home",
                "sh2.move-player-home", "sh2.bypass-max-homes", "sh2.bypass-blacklist",
                "sh2.bypass-teleport-delay", "sh2.update-notify"}) {
            assertFalse(player.hasPermission(node), node);
        }
    }

    @Test
    void stockDefaultsGiveOperatorsEverything() {
        PlayerMock op = addPlayer();
        op.setOp(true);

        for (String node : new String[]{"sh2.create-home", "sh2.manage-homes", "sh2.import-homes",
                "sh2.get-player-homes", "sh2.delete-player-home", "sh2.bypass-blacklist",
                "sh2.bypass-max-homes", "sh2.bypass-teleport-delay"}) {
            assertTrue(op.hasPermission(node), node);
        }
    }
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPlayerHomesTest extends ServerTestBase {

    @Test
    void aNonOpIsRefused() {
        PlayerMock admin = addPlayer();

        server.execute("get-player-homes", admin, "someone").assertSucceeded();

        // plugin.yml declares the command-level permission "sh2.get-player-homes"
        // (default: op), so Bukkit's command dispatcher rejects a non-op sender
        // before GetPlayerHomes#onCommand ever runs, sending its own generic
        // denial rather than ChatUtils.invalidPermissions(). "do not have
        // permission" is the substring common to both, so it holds either way.
        assertTrue(admin.nextMessage().contains("do not have permission"));
    }

    @Test
    void anOfflineOrUnknownPlayerIsReported() {
        PlayerMock admin = addPlayer();
        admin.setOp(true);

        server.execute("get-player-homes", admin, "nobody").assertSucceeded();

        assertTrue(admin.nextMessage().contains("not online"));
    }

    @Test
    void wrongArgumentCountIsReported() {
        PlayerMock admin = addPlayer();
        admin.setOp(true);

        server.execute("get-player-homes", admin).assertSucceeded();

        assertTrue(admin.nextMessage().contains("Incorrect number of arguments"));
    }

    @Test
    void anAdminSeesAnotherPlayersHomes() {
        PlayerMock target = addPlayer("target");
        PlayerMock admin = addPlayer("admin");
        admin.setOp(true);
        HomeFixtures.persist(target, "base");

        server.execute("get-player-homes", admin, "target").assertSucceeded();

        GuiSession session = plugin.getGuiSessionMap().get(admin.getUniqueId());
        assertNotNull(session);
        assertInstanceOf(HomesGui.class, session.getActiveScreen());
    }
}

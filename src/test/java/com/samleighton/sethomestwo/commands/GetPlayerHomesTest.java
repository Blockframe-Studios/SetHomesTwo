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

        assertTrue(server.execute("get-player-homes", admin, "someone").hasSucceeded());

        // Bukkit's dispatcher rejects non-op senders before onCommand runs, so this
        // is its denial message, not the plugin's. The substring matches both.
        assertTrue(admin.nextMessage().contains("do not have permission"));
    }

    @Test
    void anOfflineOrUnknownPlayerIsReported() {
        PlayerMock admin = addPlayer();
        admin.setOp(true);

        assertTrue(server.execute("get-player-homes", admin, "nobody").hasSucceeded());

        assertTrue(admin.nextMessage().contains("No player by that name"));
    }

    @Test
    void wrongArgumentCountIsReported() {
        PlayerMock admin = addPlayer();
        admin.setOp(true);

        assertTrue(server.execute("get-player-homes", admin).hasSucceeded());

        assertTrue(admin.nextMessage().contains("Incorrect number of arguments"));
    }

    @Test
    void anAdminSeesAnotherPlayersHomes() {
        PlayerMock target = addPlayer("target");
        PlayerMock admin = addPlayer("admin");
        admin.setOp(true);
        HomeFixtures.persist(target, "base");

        assertTrue(server.execute("get-player-homes", admin, "target").hasSucceeded());

        GuiSession session = plugin.getGuiSessionMap().get(admin.getUniqueId());
        assertNotNull(session);
        assertInstanceOf(HomesGui.class, session.getActiveScreen());
    }
}

package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenHomesGuiTest extends ServerTestBase {

    @Test
    void consoleIsTurnedAway() {
        assertTrue(server.executeConsole("homes").hasSucceeded());
        // The command reports back rather than doing anything.
        assertTrue(server.getConsoleSender().nextMessage().contains("Only players"));
    }

    @Test
    void aPlayerWithNoHomesIsTold() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("homes", player).hasSucceeded());

        assertTrue(player.nextMessage().contains("You have not created any homes yet."));
    }

    @Test
    void aPlayerWithHomesGetsASession() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        assertTrue(server.execute("homes", player).hasSucceeded());

        GuiSession session = plugin.getGuiSessionMap().get(player.getUniqueId());
        assertNotNull(session);
        assertNotNull(session.getActiveScreen());
    }

    @Test
    void theSessionIsReusedAcrossInvocations() {
        PlayerMock player = addPlayer();
        HomeFixtures.persist(player, "base");

        // PlayerJoin already seeds a session on join; clearing here forces the
        // first call to populate it, so assertSame actually proves reuse.
        plugin.getGuiSessionMap().clear();

        assertTrue(server.execute("homes", player).hasSucceeded());
        GuiSession first = plugin.getGuiSessionMap().get(player.getUniqueId());
        assertNotNull(first);
        // activeScreen starts null and is only set by openHomeList, so this
        // proves the command actually ran the open-list path, not just that
        // some session object happens to exist.
        assertNotNull(first.getActiveScreen());

        assertTrue(server.execute("homes", player).hasSucceeded());
        GuiSession second = plugin.getGuiSessionMap().get(player.getUniqueId());

        assertSame(first, second);
    }
}

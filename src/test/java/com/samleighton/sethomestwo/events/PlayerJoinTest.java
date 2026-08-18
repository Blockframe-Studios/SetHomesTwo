package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import com.samleighton.sethomestwo.updates.UpdateChecker;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerJoinTest extends ServerTestBase {

    @Test
    void joiningPlayerWhoMaySeeNoticesIsToldAboutAnAvailableUpdate() {
        UpdateChecker checker = new UpdateChecker(plugin, "1.2.0", () -> "v1.3.0");
        checker.checkNow();
        server.getPluginManager().registerEvents(new PlayerJoin(plugin, checker), plugin);

        TestPlayer player = addPlayer();
        player.addAttachment(plugin, UpdateChecker.NOTIFY_PERMISSION, true);
        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.empty()));

        String message = player.nextMessage();
        assertNotNull(message, "expected the join listener to deliver the update notice");
        assertTrue(message.contains("v1.3.0"));
    }

    @Test
    void joiningRefreshesTheStoredNameOnExistingHomes() {
        UpdateChecker checker = new UpdateChecker(plugin, "1.2.0", () -> null);
        server.getPluginManager().registerEvents(new PlayerJoin(plugin, checker), plugin);

        TestPlayer player = addPlayer("Steve");
        HomeFixtures.persist(player, "base");
        // Simulate a stale name left over from before this join, as if the
        // account had been seen under a different name previously.
        new HomesDao().refreshPlayerName(player.getUniqueId(), "OldSteve");

        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.empty()));

        assertEquals(player.getUniqueId().toString(), new HomesDao().uuidForName("Steve"));
    }
}

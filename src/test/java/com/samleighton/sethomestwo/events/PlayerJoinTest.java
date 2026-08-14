package com.samleighton.sethomestwo.events;

import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.support.TestPlayer;
import com.samleighton.sethomestwo.updates.UpdateChecker;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

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
        server.getPluginManager().callEvent(new PlayerJoinEvent(player, ""));

        String message = player.nextMessage();
        assertNotNull(message, "expected the join listener to deliver the update notice");
        assertTrue(message.contains("v1.3.0"));
    }
}

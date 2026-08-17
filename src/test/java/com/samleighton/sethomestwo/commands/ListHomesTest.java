package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListHomesTest extends ServerTestBase {

    @Test
    void aPlayerWithNoHomesGetsTheSameNoticeAsTheMenu() {
        PlayerMock player = addPlayer();

        assertTrue(server.execute("list-homes", player).hasSucceeded());

        String message = player.nextMessage();
        assertTrue(message.contains("You have not created any homes yet. Use /create-home to make your first one."));
        assertFalse(message.contains(ChatColor.RED.toString()), "having no homes is not an error");
        assertNull(player.nextMessage(), "one line is enough");
    }
}

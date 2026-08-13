package com.samleighton.sethomestwo.support;

import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Patches Bukkit API calls MockBukkit doesn't implement. Keep minimal: only
 * override calls the plugin never branches on, or a real failure could hide
 * behind a mock.
 */
public class TestPlayer extends PlayerMock {

    public TestPlayer(ServerMock server, String name) {
        super(server, name);
    }

    /**
     * MockBukkit 4.110.0 throws UnimplementedOperationException here. Home.teleport
     * calls it on every terminal path - success, moved-while-teleporting, and
     * unsafe-home - so without this override no teleport can complete under test.
     * Clearing a countdown title has no bearing on any assertion.
     */
    @Override
    public void resetTitle() {
        // no-op
    }
}

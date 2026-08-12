package com.samleighton.sethomestwo.support;

import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A player that tolerates the corners of the Bukkit API MockBukkit declares
 * but does not implement.
 * <p>
 * Keep this minimal. Only override calls the plugin's logic never consults,
 * and give each one a comment naming the gap it patches. Overriding something
 * the code actually branches on would hide real failures behind a mock.
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

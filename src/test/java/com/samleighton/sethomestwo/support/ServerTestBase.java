package com.samleighton.sethomestwo.support;

import com.samleighton.sethomestwo.SetHomesTwo;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

/**
 * Boots a fresh MockBukkit server and a fresh copy of the plugin for every test
 * method. Each server gets its own temporary data folder, so every test runs
 * against a clean SQLite database.
 */
@ExtendWith(FailOnUnimplemented.class)
public abstract class ServerTestBase {

    protected ServerMock server;
    protected SetHomesTwo plugin;
    protected WorldMock overworld;
    protected WorldMock nether;
    protected WorldMock end;

    @BeforeEach
    protected void startServer() {
        server = MockBukkit.mock();

        // ServerUtil maps environments onto worlds by list position, so these
        // must be added in overworld, nether, end order and before the plugin
        // has any chance to read them.
        overworld = server.addSimpleWorld("world");
        overworld.setEnvironment(World.Environment.NORMAL);

        nether = server.addSimpleWorld("world_nether");
        nether.setEnvironment(World.Environment.NETHER);

        end = server.addSimpleWorld("world_the_end");
        end.setEnvironment(World.Environment.THE_END);

        plugin = MockBukkit.load(SetHomesTwo.class);

        // Turned off before anything drains the scheduler, so the check
        // scheduled during onEnable can never reach the GitHub API.
        plugin.getConfig().set("checkForUpdates", false);
    }

    @AfterEach
    protected void stopServer() {
        MockBukkit.unmock();
    }

    /**
     * Tolerates MockBukkit's unimplemented calls. Use instead of server.addPlayer()
     * for any test that reaches Home.teleport.
     */
    protected TestPlayer addTestPlayer(String name) {
        TestPlayer player = new TestPlayer(server, name);
        server.addPlayer(player);
        return player;
    }

    /**
     * Registers a {@link TestPlayer} with a generated name. Prefer this over
     * server.addPlayer() everywhere - it only patches MockBukkit gaps, no downside.
     */
    protected TestPlayer addPlayer() {
        return addTestPlayer("TestPlayer-" + UUID.randomUUID());
    }

    /**
     * Registers a {@link TestPlayer} with the given name. See {@link #addPlayer()}.
     */
    protected TestPlayer addPlayer(String name) {
        return addTestPlayer(name);
    }
}

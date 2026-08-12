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
    }

    @AfterEach
    protected void stopServer() {
        MockBukkit.unmock();
    }

    /**
     * Register a player that tolerates MockBukkit's unimplemented calls. Use this
     * instead of server.addPlayer() for any test that reaches Home.teleport.
     *
     * @param name The player name
     * @return The registered player
     */
    protected TestPlayer addTestPlayer(String name) {
        TestPlayer player = new TestPlayer(server, name);
        server.addPlayer(player);
        return player;
    }

    /**
     * Register a {@link TestPlayer} with a generated name, mirroring
     * {@code ServerMock.addPlayer()}. Prefer this over {@code server.addPlayer()}
     * everywhere: {@link TestPlayer} only patches a MockBukkit gap nothing in the
     * plugin branches on, so there is no downside to using it by default, and it
     * removes a per-test decision about which player type a given test needs.
     *
     * @return The registered player
     */
    protected TestPlayer addPlayer() {
        return addTestPlayer("TestPlayer-" + UUID.randomUUID());
    }

    /**
     * Register a {@link TestPlayer} with the given name. See {@link #addPlayer()}.
     *
     * @param name The player name
     * @return The registered player
     */
    protected TestPlayer addPlayer(String name) {
        return addTestPlayer(name);
    }
}

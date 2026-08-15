package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlacklistTest extends ServerTestBase {

    @Test
    void addStoresTheWorld() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);

        server.execute("blacklist", player, "add", "world_nether").assertSucceeded();

        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void removeDropsTheWorld() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.remove-from-blacklist", true);

        server.execute("blacklist", player, "remove", "world_nether").assertSucceeded();

        assertFalse(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void listPrintsTheEntries() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.get-blacklisted-dimensions", true);

        server.execute("blacklist", player, "list").assertSucceeded();

        assertTrue(player.nextMessage().contains("world_nether"));
    }

    @Test
    void addIsRefusedWithoutItsOwnNode() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.get-blacklisted-dimensions", true);
        player.addAttachment(plugin, "sh2.add-to-blacklist", false);

        server.execute("blacklist", player, "add", "world_nether").assertSucceeded();

        assertTrue(player.nextMessage().contains("permission"));
        assertFalse(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void removeIsRefusedWithoutItsOwnNode() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.get-blacklisted-dimensions", true);
        player.addAttachment(plugin, "sh2.remove-from-blacklist", false);

        server.execute("blacklist", player, "remove", "world_nether").assertSucceeded();

        assertTrue(player.nextMessage().contains("permission"));
        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void listIsRefusedWithoutItsOwnNode() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);
        player.addAttachment(plugin, "sh2.remove-from-blacklist", true);
        player.addAttachment(plugin, "sh2.get-blacklisted-dimensions", false);

        server.execute("blacklist", player, "list").assertSucceeded();

        assertTrue(player.nextMessage().contains("permission"));
    }

    @Test
    void theOldCommandNameStillWorks() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);

        server.execute("add-to-blacklist", player, "add", "world_nether").assertSucceeded();

        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void anUnknownWorldIsRejected() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);

        server.execute("blacklist", player, "add", "not_a_world").assertSucceeded();

        assertTrue(player.nextMessage().contains("not a valid"));
        assertTrue(new BlacklistDao().getAll().isEmpty());
    }

    // The old command names arrive at onCommand with no subcommand token at
    // all (e.g. "/add-to-blacklist world_nether"), unlike the new "blacklist"
    // name, which always expects one. Bukkit hands onCommand the exact label
    // the player typed only when the command is dispatched through the real
    // command line, so these use server.dispatchCommand rather than
    // server.execute, which always reports the canonical command name as the
    // label regardless of which alias was used to look it up.

    @Test
    void bareAddToBlacklistAliasWithNoSubcommandStillAdds() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);

        server.dispatchCommand(player, "add-to-blacklist world_nether");

        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void bareRemoveFromBlacklistAliasWithNoSubcommandStillRemoves() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.remove-from-blacklist", true);

        server.dispatchCommand(player, "remove-from-blacklist world_nether");

        assertFalse(new BlacklistDao().getAll().contains("world_nether"));
    }

    @Test
    void bareGetBlacklistedDimensionsAliasListsEntries() {
        HomeFixtures.blacklist("world_nether");
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.get-blacklisted-dimensions", true);

        server.dispatchCommand(player, "get-blacklisted-dimensions");

        assertTrue(player.nextMessage().contains("world_nether"));
    }

    @Test
    void explicitSubcommandViaOldAliasIsNotTreatedAsAWorldName() {
        PlayerMock player = addPlayer();
        player.addAttachment(plugin, "sh2.add-to-blacklist", true);

        server.dispatchCommand(player, "add-to-blacklist add world_nether");

        assertTrue(new BlacklistDao().getAll().contains("world_nether"));
        assertFalse(new BlacklistDao().getAll().contains("add"));
    }
}

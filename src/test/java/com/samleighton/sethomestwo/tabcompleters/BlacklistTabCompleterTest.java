package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.support.HomeFixtures;
import com.samleighton.sethomestwo.support.ServerTestBase;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlacklistTabCompleterTest extends ServerTestBase {

    private List<String> complete(String label, String... args) {
        PlayerMock player = addPlayer();
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("blacklist"));

        return new BlacklistTabCompleter().onTabComplete(player, command, label, args);
    }

    @Test
    void aFragmentFromTheMiddleOfAWorldNameOffersThatWorld() {
        assertEquals(List.of("world_nether"), complete("blacklist", "add", "net"));
    }

    @Test
    void aWorldNamePrefixStillOffersThatWorld() {
        assertEquals(List.of("world_nether"), complete("blacklist", "add", "world_n"));
    }

    @Test
    void theSharedPrefixOffersEveryWorldInServerOrder() {
        assertEquals(List.of("world", "world_nether", "world_the_end"), complete("blacklist", "add", "world"));
    }

    @Test
    void aFragmentMatchingNoWorldOffersNothing() {
        assertEquals(List.of(), complete("blacklist", "add", "xyz"));
    }

    @Test
    void subcommandsMatchAnywhereToo() {
        assertEquals(List.of("remove"), complete("blacklist", "mov"));
    }

    @Test
    void removingOffersOnlyBlacklistedWorldsMatchingTheFragment() {
        HomeFixtures.blacklist("world_nether");
        HomeFixtures.blacklist("world_the_end");

        assertEquals(List.of("world_the_end"), complete("blacklist", "remove", "end"));
    }

    @Test
    void theOldAliasGoesStraightToWorldNames() {
        assertEquals(List.of("world_nether"), complete("add-to-blacklist", "net"));
    }
}

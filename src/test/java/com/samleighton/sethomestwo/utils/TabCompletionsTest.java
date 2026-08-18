package com.samleighton.sethomestwo.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabCompletionsTest {

    private static final List<String> WORLDS = List.of("world", "world_nether", "world_the_end");

    @Test
    void anEmptyFragmentOffersEverythingInSourceOrder() {
        assertEquals(WORLDS, TabCompletions.matching("", WORLDS));
    }

    @Test
    void aFragmentFromTheMiddleOfANameMatches() {
        assertEquals(List.of("world_nether"), TabCompletions.matching("net", WORLDS));
    }

    @Test
    void aPrefixStillMatches() {
        assertEquals(List.of("world_nether"), TabCompletions.matching("world_n", WORLDS));
    }

    @Test
    void matchingIgnoresCase() {
        assertEquals(List.of("world_nether"), TabCompletions.matching("NET", WORLDS));
        // The candidate keeps its own casing in the result.
        assertEquals(List.of("World_Nether"), TabCompletions.matching("net", List.of("World_Nether")));
    }

    @Test
    void prefixMatchesRankAboveSubstringMatches() {
        // "minecart" contains "cart" and comes first in source order, but
        // "cart" starts with it and must be offered first.
        assertEquals(List.of("cart", "minecart"), TabCompletions.matching("cart", List.of("minecart", "cart")));
    }

    @Test
    void sourceOrderIsKeptWithinEachTier() {
        List<String> source = List.of("stone_axe", "axe", "sandstone", "stone");
        assertEquals(List.of("stone_axe", "stone", "sandstone"), TabCompletions.matching("stone", source));
    }

    @Test
    void aNameIsOfferedOnce() {
        assertEquals(List.of("stone"), TabCompletions.matching("stone", List.of("stone", "stone")));
    }

    @Test
    void aFragmentThatMatchesNothingOffersNothing() {
        assertEquals(List.of(), TabCompletions.matching("xyz", WORLDS));
    }
}

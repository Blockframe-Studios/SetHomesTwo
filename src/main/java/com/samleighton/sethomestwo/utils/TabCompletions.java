package com.samleighton.sethomestwo.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Tab completion matching shared by every completer in the plugin.
 */
public final class TabCompletions {

    private TabCompletions() {
    }

    /**
     * Returns the candidates that contain the fragment, case-insensitively.
     * Candidates that start with the fragment come first, then the rest;
     * source order is kept within each group and duplicates are dropped.
     *
     * @param fragment   what the player has typed so far; empty matches everything
     * @param candidates the names to choose from
     * @return the matching candidates, in ranked order, never null
     */
    public static List<String> matching(String fragment, Collection<String> candidates) {
        String needle = fragment.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> prefixHits = new LinkedHashSet<>();
        LinkedHashSet<String> substringHits = new LinkedHashSet<>();

        for (String candidate : candidates) {
            String haystack = candidate.toLowerCase(Locale.ROOT);
            if (haystack.startsWith(needle)) {
                prefixHits.add(candidate);
            } else if (haystack.contains(needle)) {
                substringHits.add(candidate);
            }
        }

        List<String> matches = new ArrayList<>(prefixHits);
        for (String hit : substringHits) {
            if (!prefixHits.contains(hit)) matches.add(hit);
        }
        return matches;
    }
}

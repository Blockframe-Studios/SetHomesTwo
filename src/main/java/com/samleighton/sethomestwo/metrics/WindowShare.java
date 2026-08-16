package com.samleighton.sethomestwo.metrics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets several charts report the same submission window of one counter family
 * without draining it more than once. bStats runs every chart callback in
 * registration order within one submission, so the bar chart drains and parks
 * the window, and the line charts registered after it read the parked copy.
 */
final class WindowShare {

    private final UsageCounters counters;
    private final Map<UsageCounters.Family, Map<String, Integer>> parked = new EnumMap<>(UsageCounters.Family.class);

    WindowShare(UsageCounters counters) {
        this.counters = counters;
    }

    /**
     * Drains the family into the one-value-per-bar shape AdvancedBarChart wants
     * and parks the window for {@link #count} and {@link #total}.
     */
    Map<String, int[]> bars(UsageCounters.Family family) {
        Map<String, Integer> window = counters.snapshotAndReset(family);
        parked.put(family, window);
        Map<String, int[]> out = new HashMap<>();
        window.forEach((key, count) -> out.put(key, new int[]{count}));
        return out;
    }

    /** One key's count in the parked window, 0 when absent. */
    int count(UsageCounters.Family family, String key) {
        return window(family).getOrDefault(key, 0);
    }

    /** Sum of every key in the parked window. */
    int total(UsageCounters.Family family) {
        int sum = 0;
        for (int value : window(family).values()) sum += value;
        return sum;
    }

    /**
     * The parked window, draining the live counters if no bar chart parked one
     * first so a line chart registered on its own still reports.
     */
    private Map<String, Integer> window(UsageCounters.Family family) {
        return parked.computeIfAbsent(family, counters::snapshotAndReset);
    }
}

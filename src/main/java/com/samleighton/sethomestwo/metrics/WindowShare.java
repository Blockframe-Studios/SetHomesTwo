package com.samleighton.sethomestwo.metrics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Lets several charts report the same submission window of one counter family
 * without draining it more than once. bStats keeps its charts in a hash set,
 * so callback order is arbitrary: the first reader of a family in a submission
 * drains and parks the window, and every reader within {@link #SAME_SUBMISSION}
 * of that gets the parked copy. Submissions are 30 minutes apart and all
 * callbacks of one submission run within milliseconds, so the span cannot
 * bridge two windows.
 */
final class WindowShare {

    static final long SAME_SUBMISSION = TimeUnit.SECONDS.toNanos(60);

    private final UsageCounters counters;
    private final LongSupplier nanoTime;
    private final Map<UsageCounters.Family, Map<String, Integer>> parked = new EnumMap<>(UsageCounters.Family.class);
    private final Map<UsageCounters.Family, Long> parkedAt = new EnumMap<>(UsageCounters.Family.class);

    WindowShare(UsageCounters counters) {
        this(counters, System::nanoTime);
    }

    WindowShare(UsageCounters counters, LongSupplier nanoTime) {
        this.counters = counters;
        this.nanoTime = nanoTime;
    }

    /** The window in the one-value-per-bar shape AdvancedBarChart wants. */
    Map<String, int[]> bars(UsageCounters.Family family) {
        Map<String, int[]> out = new HashMap<>();
        window(family).forEach((key, count) -> out.put(key, new int[]{count}));
        return out;
    }

    /** One key's count in the window, 0 when absent. */
    int count(UsageCounters.Family family, String key) {
        return window(family).getOrDefault(key, 0);
    }

    /** Sum of every key in the window. */
    int total(UsageCounters.Family family) {
        int sum = 0;
        for (int value : window(family).values()) sum += value;
        return sum;
    }

    private Map<String, Integer> window(UsageCounters.Family family) {
        long now = nanoTime.getAsLong();
        Long at = parkedAt.get(family);
        if (at == null || now - at > SAME_SUBMISSION) {
            parked.put(family, counters.snapshotAndReset(family));
            parkedAt.put(family, now);
        }
        return parked.get(family);
    }
}

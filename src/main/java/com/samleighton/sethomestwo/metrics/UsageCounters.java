package com.samleighton.sethomestwo.metrics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory usage counters, one map per family. Increments are lock-free and
 * safe from any thread; a snapshot is what bStats reads once per submission.
 */
public class UsageCounters {

    public enum Family { COMMAND, ALIAS, GUI_ACTION, TELEPORT_SOURCE, TELEPORT_OUTCOME, ERROR }

    public static final String SOURCE_GUI = "gui";
    public static final String SOURCE_COMMAND = "command";

    public static final String OUTCOME_BLACKLISTED = "blacklisted";
    public static final String OUTCOME_ALREADY_TELEPORTING = "already-teleporting";
    public static final String OUTCOME_CANCELLED_MOVED = "cancelled-moved";
    public static final String OUTCOME_UNSAFE = "unsafe";
    public static final String OUTCOME_RELOCATED = "relocated";
    public static final String OUTCOME_COMPLETED = "completed";

    public static final String GUI_TELEPORT = "teleport";
    public static final String GUI_PAGE_NEXT = "page-next";
    public static final String GUI_PAGE_PREVIOUS = "page-previous";

    private final Map<Family, ConcurrentHashMap<String, LongAdder>> families = new EnumMap<>(Family.class);

    public UsageCounters() {
        for (Family family : Family.values()) {
            families.put(family, new ConcurrentHashMap<>());
        }
    }

    public void increment(Family family, String key) {
        families.get(family).computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    /**
     * Current counts for the family, without clearing them.
     */
    public Map<String, Integer> snapshot(Family family) {
        Map<String, Integer> out = new HashMap<>();
        families.get(family).forEach((key, adder) -> {
            int value = (int) Math.min(Integer.MAX_VALUE, adder.sum());
            if (value > 0) out.put(key, value);
        });
        return out;
    }

    /**
     * Current counts for the family, then reset so the next window starts at zero.
     * This is the shape a bStats AdvancedPie callback returns.
     */
    public Map<String, Integer> snapshotAndReset(Family family) {
        Map<String, Integer> out = new HashMap<>();
        ConcurrentHashMap<String, LongAdder> map = families.get(family);
        for (String key : map.keySet()) {
            LongAdder adder = map.remove(key);
            if (adder == null) continue;
            int value = (int) Math.min(Integer.MAX_VALUE, adder.sum());
            if (value > 0) out.put(key, value);
        }
        return out;
    }
}

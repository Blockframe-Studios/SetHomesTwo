package com.samleighton.sethomestwo.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.ALIAS;
import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.COMMAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageCountersTest {

    @Test
    void incrementsAccumulatePerKey() {
        UsageCounters counters = new UsageCounters();

        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "create-home");

        Map<String, Integer> snapshot = counters.snapshot(COMMAND);
        assertEquals(2, snapshot.get("go-home"));
        assertEquals(1, snapshot.get("create-home"));
        assertEquals(2, snapshot.size());
    }

    @Test
    void snapshotAndResetReturnsTheCountsAndClearsThem() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");

        Map<String, Integer> first = counters.snapshotAndReset(COMMAND);
        assertEquals(1, first.get("go-home"));

        assertTrue(counters.snapshotAndReset(COMMAND).isEmpty());
    }

    @Test
    void familiesAreIndependent() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        counters.increment(ALIAS, "home");

        counters.snapshotAndReset(COMMAND);

        assertEquals(1, counters.snapshot(ALIAS).get("home"));
        assertTrue(counters.snapshot(COMMAND).isEmpty());
    }

    @Test
    void anEmptyFamilySnapshotsToAnEmptyMap() {
        assertTrue(new UsageCounters().snapshot(COMMAND).isEmpty());
    }
}

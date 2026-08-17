package com.samleighton.sethomestwo.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.COMMAND;
import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.GUI_ACTION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowShareTest {

    private final AtomicLong nanos = new AtomicLong(TimeUnit.HOURS.toNanos(1));

    private WindowShare share(UsageCounters counters) {
        return new WindowShare(counters, nanos::get);
    }

    private void later(long seconds) {
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
    }

    @Test
    void everyReaderInOneSubmissionSeesTheSameWindowWhateverTheOrder() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "create-home");
        WindowShare share = share(counters);

        // A line chart happens to run first: bStats keeps charts in a hash set.
        assertEquals(2, share.count(COMMAND, "go-home"));
        assertTrue(counters.snapshot(COMMAND).isEmpty(), "the first reader drains the family");

        Map<String, int[]> bars = share.bars(COMMAND);
        assertArrayEquals(new int[]{2}, bars.get("go-home"));
        assertArrayEquals(new int[]{1}, bars.get("create-home"));

        assertEquals(3, share.total(COMMAND));
        assertEquals(1, share.count(COMMAND, "create-home"));
        assertEquals(0, share.count(COMMAND, "delete-home"));
    }

    @Test
    void theNextSubmissionDrainsAFreshWindow() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        WindowShare share = share(counters);
        assertEquals(1, share.total(COMMAND));

        counters.increment(COMMAND, "create-home");
        later(59);
        assertEquals(1, share.total(COMMAND), "still the same submission");

        later(2);
        assertEquals(1, share.total(COMMAND), "next submission: only what arrived since");
        assertEquals(1, share.count(COMMAND, "create-home"));
        assertEquals(0, share.count(COMMAND, "go-home"));
        assertTrue(share.bars(COMMAND).containsKey("create-home"));
    }

    @Test
    void anEmptyWindowIsStillOneWindow() {
        UsageCounters counters = new UsageCounters();
        WindowShare share = share(counters);

        assertTrue(share.bars(COMMAND).isEmpty());
        counters.increment(COMMAND, "go-home");
        assertEquals(0, share.total(COMMAND), "arrived after this submission drained");

        later(120);
        assertEquals(1, share.total(COMMAND));
    }

    @Test
    void familiesDoNotLeakIntoEachOther() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        WindowShare share = share(counters);

        share.bars(COMMAND);
        assertEquals(0, share.total(GUI_ACTION));
        assertEquals(1, share.total(COMMAND));
    }
}

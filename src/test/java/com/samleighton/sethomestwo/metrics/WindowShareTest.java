package com.samleighton.sethomestwo.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.COMMAND;
import static com.samleighton.sethomestwo.metrics.UsageCounters.Family.GUI_ACTION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowShareTest {

    @Test
    void barsDrainTheFamilyAndParkTheWindowForTheLineReaders() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "go-home");
        counters.increment(COMMAND, "create-home");
        WindowShare share = new WindowShare(counters);

        Map<String, int[]> bars = share.bars(COMMAND);
        assertArrayEquals(new int[]{2}, bars.get("go-home"));
        assertArrayEquals(new int[]{1}, bars.get("create-home"));
        assertTrue(counters.snapshot(COMMAND).isEmpty(), "bars must reset the window");

        assertEquals(2, share.count(COMMAND, "go-home"));
        assertEquals(1, share.count(COMMAND, "create-home"));
        assertEquals(0, share.count(COMMAND, "delete-home"));
        assertEquals(3, share.total(COMMAND));

        // Any number of readers may look at the same parked window.
        assertEquals(2, share.count(COMMAND, "go-home"));
        assertEquals(3, share.total(COMMAND));
    }

    @Test
    void theNextBarsCallReplacesTheParkedWindow() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        WindowShare share = new WindowShare(counters);
        share.bars(COMMAND);

        counters.increment(COMMAND, "create-home");
        share.bars(COMMAND);

        assertEquals(0, share.count(COMMAND, "go-home"));
        assertEquals(1, share.count(COMMAND, "create-home"));
        assertEquals(1, share.total(COMMAND));
    }

    @Test
    void aReaderWithoutAPrecedingDrainDrainsTheLiveWindowItself() {
        UsageCounters counters = new UsageCounters();
        counters.increment(GUI_ACTION, "back");
        WindowShare share = new WindowShare(counters);

        assertEquals(1, share.total(GUI_ACTION));
        assertTrue(counters.snapshot(GUI_ACTION).isEmpty());
        assertEquals(1, share.count(GUI_ACTION, "back"), "the drained window stays parked");
    }

    @Test
    void familiesDoNotLeakIntoEachOther() {
        UsageCounters counters = new UsageCounters();
        counters.increment(COMMAND, "go-home");
        WindowShare share = new WindowShare(counters);

        share.bars(COMMAND);
        assertEquals(0, share.total(GUI_ACTION));
        assertEquals(1, share.total(COMMAND));
    }
}

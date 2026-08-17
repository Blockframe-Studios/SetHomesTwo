package com.samleighton.sethomestwo.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BucketsTest {

    @Test
    void delayBuckets() {
        assertEquals("0", Buckets.delay(0));
        assertEquals("0", Buckets.delay(-1));
        assertEquals("1-3", Buckets.delay(1));
        assertEquals("1-3", Buckets.delay(3));
        assertEquals("4-10", Buckets.delay(4));
        assertEquals("4-10", Buckets.delay(10));
        assertEquals("10+", Buckets.delay(11));
    }

    @Test
    void homesPerServerBuckets() {
        assertEquals("0", Buckets.homesPerServer(0));
        assertEquals("1-50", Buckets.homesPerServer(1));
        assertEquals("1-50", Buckets.homesPerServer(50));
        assertEquals("51-500", Buckets.homesPerServer(51));
        assertEquals("51-500", Buckets.homesPerServer(500));
        assertEquals("501-5000", Buckets.homesPerServer(501));
        assertEquals("501-5000", Buckets.homesPerServer(5000));
        assertEquals("5000+", Buckets.homesPerServer(5001));
    }

    @Test
    void homesPerPlayerBuckets() {
        assertEquals("0", Buckets.homesPerPlayer(0, 0));
        assertEquals("1", Buckets.homesPerPlayer(3, 3));
        assertEquals("1", Buckets.homesPerPlayer(5, 3));
        assertEquals("2-3", Buckets.homesPerPlayer(6, 3));
        assertEquals("2-3", Buckets.homesPerPlayer(11, 3));
        assertEquals("4-10", Buckets.homesPerPlayer(12, 3));
        assertEquals("4-10", Buckets.homesPerPlayer(30, 3));
        assertEquals("10+", Buckets.homesPerPlayer(33, 3));
    }
}

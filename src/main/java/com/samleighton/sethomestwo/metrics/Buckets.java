package com.samleighton.sethomestwo.metrics;

/**
 * Bucket labels for the bStats scale and config pies. Ranges are what the
 * dashboard shows, so change them only with a matching change to the README.
 */
public final class Buckets {

    private Buckets() {}

    public static String delay(int seconds) {
        if (seconds <= 0) return "0";
        if (seconds <= 3) return "1-3";
        if (seconds <= 10) return "4-10";
        return "10+";
    }

    public static String homesPerServer(int total) {
        if (total <= 0) return "0";
        if (total <= 50) return "1-50";
        if (total <= 500) return "51-500";
        if (total <= 5000) return "501-5000";
        return "5000+";
    }

    /**
     * Average homes per player with at least one home, rounded down.
     */
    public static String homesPerPlayer(int total, int players) {
        if (total <= 0 || players <= 0) return "0";
        int average = total / players;
        if (average <= 1) return "1";
        if (average <= 3) return "2-3";
        if (average <= 10) return "4-10";
        return "10+";
    }
}

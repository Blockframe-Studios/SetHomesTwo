package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.SetHomesTwo;

/**
 * Counts plugin failures by category for the bStats errors chart. Categories
 * only, never messages, so nothing server-specific leaves the server.
 */
public final class Errors {

    public static final String SQL_WRITE = "sql-write";
    public static final String SQL_READ = "sql-read";
    public static final String DB_CONNECT = "db-connect";
    public static final String ITEM_DATA = "item-data";

    private Errors() {}

    /**
     * Safe to call from anywhere the plugin logs a failure; a null plugin
     * (before it is registered) is ignored.
     */
    public static void count(String category) {
        SetHomesTwo plugin = SetHomesTwo.instance();
        if (plugin != null) plugin.getUsageCounters().increment(UsageCounters.Family.ERROR, category);
    }
}

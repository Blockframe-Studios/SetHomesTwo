package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.support.ServerTestBase;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorsTest extends ServerTestBase {

    private Map<String, Integer> errors() {
        return plugin.getUsageCounters().snapshot(UsageCounters.Family.ERROR);
    }

    private Connection homes() {
        return plugin.getConnectionManager().getConnection("homes");
    }

    @Test
    void aFailedWriteCountsAsSqlWrite() {
        assertFalse(DatabaseUtil.execute(homes(), "insert into no_such_table values (1)"));
        assertEquals(-1, DatabaseUtil.executeUpdate(homes(), "update no_such_table set x = 1"));

        assertEquals(2, errors().get(Errors.SQL_WRITE));
        assertFalse(errors().containsKey(Errors.SQL_READ));
    }

    @Test
    void aFailedReadCountsAsSqlRead() {
        assertNull(DatabaseUtil.fetch(homes(), "select * from no_such_table"));

        assertEquals(1, errors().get(Errors.SQL_READ));
    }

    @Test
    void successfulStatementsCountNothing() {
        assertTrue(DatabaseUtil.execute(homes(), "select 1"));
        assertTrue(errors().isEmpty());
    }

    @Test
    void theStaticHookLandsInTheErrorFamily() {
        Errors.count(Errors.DB_CONNECT);
        assertEquals(1, errors().get(Errors.DB_CONNECT));
    }
}

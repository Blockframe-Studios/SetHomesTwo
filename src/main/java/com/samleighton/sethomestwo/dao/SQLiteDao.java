package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.SetHomesTwo;

import java.sql.Connection;

public abstract class SQLiteDao {

    public final Connection conn;

    public SQLiteDao(){
        this.conn = SetHomesTwo.instance().getConnectionManager().getConnection("homes");
    }
}

package com.samleighton.sethomestwo.connections;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.metrics.Errors;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {
    private final Map<String, Connection> activeConnections;

    public ConnectionManager() {
        activeConnections = new HashMap<>();
    }

    public Connection getConnection(String key) {
        return activeConnections.get(key);
    }

    public void addConnection(String key, Connection connection) {
        activeConnections.put(key, connection);
    }

    public boolean createConnection(String key, String dbName) {
        SetHomesTwo plugin = SetHomesTwo.instance();
        String dbURL = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database/" + dbName + ".db";

        try {
            Connection connection = DriverManager.getConnection(dbURL);
            addConnection(key, connection);
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe(String.format("There was an issue creating the database %s", dbName));
            Errors.count(Errors.DB_CONNECT);
        }

        return false;
    }

    /**
     * Close all active connections
     */
    public void closeConnections() {
        for (Connection conn : activeConnections.values()) {
            if (conn == null) continue;

            try {
                conn.close();
            } catch (SQLException e) {
                Bukkit.getLogger().severe("There was an issue closing a database connection.");
            }
        }
    }
}

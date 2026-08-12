package com.samleighton.sethomestwo.connections;

import com.samleighton.sethomestwo.SetHomesTwo;
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
        // Looked up by name via the plugin manager rather than
        // JavaPlugin.getPlugin(SetHomesTwo.class): MockBukkit enables the
        // plugin through a generated subclass loaded by its own classloader,
        // so the literal SetHomesTwo.class reference never satisfies that
        // method's same-classloader check under test.
        SetHomesTwo plugin = (SetHomesTwo) Bukkit.getPluginManager().getPlugin("SetHomesTwo");
        String dbURL = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database/" + dbName + ".db";

        try {
            Connection connection = DriverManager.getConnection(dbURL);
            addConnection(key, connection);
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe(String.format("There was an issue creating the database %s", dbName));
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

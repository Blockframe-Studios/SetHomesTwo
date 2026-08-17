package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.metrics.Errors;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.List;

public class DatabaseUtil {

    public static boolean initTables(Connection connection){
        if(connection == null) return false;

        // Create players_homes table
        String createPlayersHomesSQL = "create table if not exists %s (\n" +
                "id integer PRIMARY KEY, \n" +
                "player_uuid TEXT NOT NULL, \n" +
                "material TEXT NOT NULL, \n" +
                "world TEXT NOT NULL, \n" +
                "name TEXT NOT NULL, \n" +
                "description TEXT, \n" +
                "x real NOT NULL, \n" +
                "y real NOT NULL, \n" +
                "z real NOT NULL, \n" +
                "pitch real NOT NULL, \n" +
                "yaw real NOT NULL, \n" +
                "dimension TEXT" +
                ");";
        boolean createPlayersHomes = execute(connection, String.format(createPlayersHomesSQL, "players_homes"));

        boolean playerNameColumn = ensureColumn(connection, "players_homes", "player_name", "TEXT");

        // Create blacklist table
        String createBlacklistSQL = "create table if not exists %s (\n" +
                "id integer PRIMARY KEY, \n" +
                "dimension_name TEXT NOT NULL \n" +
                ");";
        boolean createBlacklist = execute(connection, String.format(createBlacklistSQL, "blacklist"));

        // Create player_teleport_attempts table
        String createSQL = "create table if not exists %s (\n" +
                "id integer PRIMARY KEY, \n" +
                "player_uuid TEXT NOT NULL UNIQUE, \n" +
                "world TEXT NOT NULL, \n" +
                "x real NOT NULL, \n" +
                "y real NOT NULL, \n" +
                "z real NOT NULL \n" +
                ");";
        boolean createPlayerTeleportAttempts = execute(connection, String.format(createSQL, "player_teleport_attempts"));

        return createPlayerTeleportAttempts && createBlacklist && createPlayersHomes && playerNameColumn;
    }

    /**
     * SQLite has no ADD COLUMN IF NOT EXISTS, so the column list is read first.
     */
    private static boolean ensureColumn(Connection connection, String table, String column, String type) {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("pragma table_info(" + table + ");")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not read columns for " + table + ": " + e.getMessage());
            return false;
        }

        return execute(connection, String.format("alter table %s add column %s %s;", table, column, type));
    }

    /**
     * Execute a query on the database.
     *
     * @param connection, The connection to execute this query on.
     * @param sql,        The query to execute.
     * @param params,     Parameters to bind to the statement.
     * @return boolean
     */
    public static boolean execute(Connection connection, String sql, Object... params) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            // Set optional params and execute
            setParams(statement, params);
            statement.execute();
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not execute sql statement.");
            Errors.count(Errors.SQL_WRITE);
        }

        return false;
    }

    /**
     * Execute a statement and report how many rows it changed.
     *
     * @param connection The database connection
     * @param sql        The sql string to execute
     * @param params     Optional bind parameters
     * @return The affected row count, or -1 when the statement failed
     */
    public static int executeUpdate(Connection connection, String sql, Object... params) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            setParams(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not execute sql update statement.");
            Errors.count(Errors.SQL_WRITE);
        }

        return -1;
    }

    /**
     * Fetch a set of results from the database.
     *
     * @param connection, The database connection to fetch from
     * @param sql,        The sql string used for querying
     * @param params,     Optional sql parameters
     * @return ResultSet
     */
    @Nullable
    public static ResultSet fetch(Connection connection, String sql, Object... params) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            setParams(statement, params);

            return statement.executeQuery();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not execute sql fetch statement.");
            Errors.count(Errors.SQL_READ);
        }

        return null;
    }


    /**
     * Bind parameters to a prepared statement.
     * @param statement, The statement to bind to.
     * @param params, The parameters to bind.
     */
    public static void setParams(PreparedStatement statement, Object... params) {
        if (params.length == 0) return;

        try {
            int paramIndex = 1;
            for (Object param : params) {
                if (param instanceof List) {
                    for (Object p : ((List<?>) param).toArray()) {
                        setParam(statement, paramIndex, p);
                        paramIndex++;
                    }
                } else {
                    setParam(statement, paramIndex, param);
                    paramIndex++;
                }
            }

            if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("STMT: " + statement.toString());
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not bind parameter to SQL statement!");

            if(ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info(e.getMessage());
        }
    }

    /**
     * Attempts to bind values with their respective placeholders in a prepared sql statement.
     *
     * @param statement,  The statement to bind with
     * @param paramIndex, The index to add the parameter at
     * @param param,      The parameters to bind
     */
    private static void setParam(PreparedStatement statement, int paramIndex, Object param) throws SQLException {
        if (param == null) {
            statement.setNull(paramIndex, Types.NULL);
        }

        if (param instanceof Integer) {
            statement.setInt(paramIndex, (int) param);
        }

        if (param instanceof String) {
            statement.setString(paramIndex, (String) param);
        }

        if (param instanceof Double) {
            statement.setDouble(paramIndex, (double) param);
        }

        if (param instanceof Float) {
            statement.setFloat(paramIndex, (float) param);
        }
    }
}

package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomesDao extends SQLiteDao implements Dao<Home> {
    private final String TABLE_NAME = "players_homes";
    private boolean isAdmin = false;
    public HomesDao(){
        super();
    }

    public HomesDao(boolean isAdmin){
        super();
        this.isAdmin = isAdmin;
    }

    @Override
    public List<Home> getAll(Object... keys) {
        // Convert keys
        UUID playerUUID = null;
        for(Object key : keys){
            if(key instanceof UUID) playerUUID = (UUID) key;
        }

        // Model key not found
        if(playerUUID == null) return null;

        // Build query and fetch
        String sql = "select * from %s where player_uuid = ?";
        ResultSet rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString());

        if (rs == null) return new ArrayList<>();

        // Build list of homes
        // Read once, not once per row.
        List<String> blacklistedWorlds = new BlacklistDao().getAll();
        List<Home> playerHomes = new ArrayList<>();
        try {
            while (rs.next()) {
                Location homeLocation = new Location(
                        Bukkit.getWorld(UUID.fromString(rs.getString("world"))),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
                Home home = new Home(
                        rs.getString("player_uuid"),
                        rs.getString("material"),
                        homeLocation,
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("dimension")
                );
                home.setId(rs.getInt("id"));
                home.setPlayerName(rs.getString("player_name"));

                World homeWorld = Bukkit.getWorld(UUID.fromString(home.getWorld()));
                if (ServerUtil.isWorldBlacklisted(homeWorld, blacklistedWorlds)) {
                    if (!this.isAdmin) home.setDescription("Cannot teleport here: dimension blacklisted");

                    home.setCanTeleport(this.isAdmin);
                }

                playerHomes.add(home);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("There was an issue reading homes for player " + playerUUID);
            Bukkit.getLogger().info(e.getMessage());
        }

        return playerHomes;
    }

    /**
     * Look a home up by owner and name. The name match ignores case, which is
     * safe because {@link #nameExists} makes names unique per player ignoring
     * case, so at most one home can ever match.
     */
    @Override
    public Home get(Object... keys) {
        UUID playerUUID = null;
        String homeName = null;

        for(Object key : keys){
            if(key instanceof UUID) playerUUID = (UUID) key;
            if(key instanceof String) homeName = (String) key;
        }

        // Key guard
        if(homeName == null || playerUUID == null) return null;

        String sql = "select * from %s where player_uuid = ? and lower(name) = lower(?)";
        ResultSet rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString(), homeName);

        if(rs == null) return null;

        Home home = null;

        try {
            while(rs.next()){
                Location homeLocation = new Location(
                        Bukkit.getWorld(UUID.fromString(rs.getString("world"))),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
                home = new Home(
                        rs.getString("player_uuid"),
                        rs.getString("material"),
                        homeLocation,
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("dimension")
                );
                home.setId(rs.getInt("id"));
                home.setPlayerName(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("There was an issue reading a home for player " + playerUUID);
            Bukkit.getLogger().info(e.getMessage());
        }

        return home;
    }

    @Override
    public boolean save(Object object) {
        if(!(object instanceof Home)) return false;

        Home home = (Home) object;

        String playerName = home.getPlayerName();
        if (playerName == null) {
            Player owner = Bukkit.getPlayer(UUID.fromString(home.getUUIDBelongingTo()));
            playerName = owner == null ? null : owner.getName();
        }

        String sql = "insert into %s (player_uuid, world, material, name, description, x, y, z, pitch, yaw, dimension, player_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        return DatabaseUtil.execute(
                this.conn,
                String.format(sql, TABLE_NAME),
                home.getUUIDBelongingTo(),
                home.getWorld(),
                home.getMaterial(),
                home.getName(),
                home.getDescription(),
                home.getX(),
                home.getY(),
                home.getZ(),
                home.getPitch(),
                home.getYaw(),
                home.getDimension(),
                playerName
        );
    }

    @Override
    public boolean delete(Object object) {
        if (!(object instanceof Home)) return false;

        Home homeToRemove = (Home) object;

        if (homeToRemove.getId() == null) {
            Bukkit.getLogger().severe("Refusing to delete a home that has no id.");
            return false;
        }

        String sql = "delete from %s where id = ? and player_uuid = ?";
        return DatabaseUtil.executeUpdate(this.conn, String.format(sql, TABLE_NAME), homeToRemove.getId(), homeToRemove.getUUIDBelongingTo()) > 0;
    }

    @Override
    public boolean update(Object object) {
        if (!(object instanceof Home)) return false;

        Home home = (Home) object;

        if (home.getId() == null) {
            Bukkit.getLogger().severe("Refusing to update a home that has no id.");
            return false;
        }

        String playerName = home.getPlayerName();
        if (playerName == null) {
            Player owner = Bukkit.getPlayer(UUID.fromString(home.getUUIDBelongingTo()));
            playerName = owner == null ? null : owner.getName();
        }

        String sql = "update %s set material = ?, world = ?, name = ?, description = ?, x = ?, y = ?, z = ?, pitch = ?, yaw = ?, dimension = ?, player_name = ? where id = ? and player_uuid = ?";
        return DatabaseUtil.executeUpdate(
                this.conn,
                String.format(sql, TABLE_NAME),
                home.getMaterial(),
                home.getWorld(),
                home.getName(),
                home.getDescription(),
                home.getX(),
                home.getY(),
                home.getZ(),
                home.getPitch(),
                home.getYaw(),
                home.getDimension(),
                playerName,
                home.getId(),
                home.getUUIDBelongingTo()
        ) > 0;
    }

    /**
     * Fetch a single home by its primary key, scoped to its owner so one player
     * can never address another player's home by guessing an id.
     *
     * @param playerUUID The owning player
     * @param id         The players_homes primary key
     * @return Home, or null when no such row exists
     */
    public Home getById(UUID playerUUID, int id) {
        String sql = "select * from %s where player_uuid = ? and id = ?";
        ResultSet rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString(), id);

        if (rs == null) return null;

        try {
            if (rs.next()) {
                Location homeLocation = new Location(
                        Bukkit.getWorld(UUID.fromString(rs.getString("world"))),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
                Home home = new Home(
                        rs.getString("player_uuid"),
                        rs.getString("material"),
                        homeLocation,
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("dimension")
                );
                home.setId(rs.getInt("id"));
                home.setPlayerName(rs.getString("player_name"));
                return home;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("There was an issue reading home id " + id);
            Bukkit.getLogger().info(e.getMessage());
        }

        return null;
    }

    /**
     * Case-insensitive check for whether this player already has a home by this
     * name.
     *
     * @param playerUUID The owning player
     * @param name       The candidate name
     * @param excludeId  A home id to ignore, so a home renaming to its own
     *                   current name does not conflict with itself. Pass null
     *                   when creating a new home.
     * @return true when the name is already taken
     */
    public boolean nameExists(UUID playerUUID, String name, Integer excludeId) {
        if (name == null) return false;

        String sql = "select count(*) as total from %s where player_uuid = ? and lower(name) = lower(?)";
        ResultSet rs;

        if (excludeId == null) {
            rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString(), name);
        } else {
            rs = DatabaseUtil.fetch(this.conn, String.format(sql + " and id != ?", TABLE_NAME), playerUUID.toString(), name, excludeId);
        }

        if (rs == null) return false;

        try {
            if (rs.next()) return rs.getInt("total") > 0;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("There was an issue checking for a duplicate home name.");
            Bukkit.getLogger().info(e.getMessage());
        }

        return false;
    }

    /**
     * The UUID of the player who owns homes stored under this name, or null.
     * A stale name can collide across two accounts (an old owner who renamed
     * away and a new owner who took the name); returns null rather than
     * guessing when more than one distinct UUID claims the name.
     */
    public String uuidForName(String playerName) {
        String sql = "select distinct player_uuid from players_homes where player_name = ?;";

        try (PreparedStatement statement = this.conn.prepareStatement(sql)) {
            statement.setString(1, playerName);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;

                String uuid = rs.getString("player_uuid");
                return rs.next() ? null : uuid;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not resolve player name " + playerName);
            return null;
        }
    }

    /**
     * Point every home this player owns at their current name, first stripping
     * that name from any other UUID's rows so a stale prior owner can never
     * make the name resolve ambiguously. The joining player takes precedence.
     */
    public boolean refreshPlayerName(UUID playerUUID, String playerName) {
        String clearSql = "update players_homes set player_name = null where player_name = ? and player_uuid <> ?;";
        String claimSql = "update players_homes set player_name = ? where player_uuid = ?;";

        try (PreparedStatement clear = this.conn.prepareStatement(clearSql);
             PreparedStatement claim = this.conn.prepareStatement(claimSql)) {
            clear.setString(1, playerName);
            clear.setString(2, playerUUID.toString());
            clear.executeUpdate();

            claim.setString(1, playerName);
            claim.setString(2, playerUUID.toString());
            claim.executeUpdate();
            return true;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not refresh player name for " + playerUUID);
            return false;
        }
    }
}

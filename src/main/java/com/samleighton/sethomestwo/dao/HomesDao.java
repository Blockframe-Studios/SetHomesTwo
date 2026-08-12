package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        Dao<String> blacklistEntryDao = new BlacklistDao();
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

                String dimension = home.getDimension();
                List<String> blacklistedDimensions = blacklistEntryDao.getAll();
                Map<String, String> blacklistedMap = ServerUtil.getDimensionsMap();

                if (blacklistedDimensions.contains(blacklistedMap.get(dimension))) {
                    if(!this.isAdmin) home.setDescription("Cannot teleport here: dimension blacklisted");

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

        String sql = "select * from %s where player_uuid = ? and name = ?";
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
        String sql = "insert into %s (player_uuid, world, material, name, description, x, y, z, pitch, yaw, dimension) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
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
                home.getDimension()
        );
    }

    @Override
    public boolean delete(Object object) {
        if(!(object instanceof Home)) return false;

        Home homeToRemove = (Home) object;
        String sql = "delete from %s where player_uuid = ? and name = ?";
        return DatabaseUtil.execute(this.conn, String.format(sql, TABLE_NAME), homeToRemove.getUUIDBelongingTo(), homeToRemove.getName());
    }
}

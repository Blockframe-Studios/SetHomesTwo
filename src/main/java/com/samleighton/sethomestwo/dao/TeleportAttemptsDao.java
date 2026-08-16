package com.samleighton.sethomestwo.dao;

import com.samleighton.sethomestwo.models.TeleportAttempt;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TeleportAttemptsDao extends SQLiteDao implements Dao<TeleportAttempt> {
    private final String TABLE_NAME = "player_teleport_attempts";

    public TeleportAttemptsDao() {
        super();
    }

    @Override
    public List<TeleportAttempt> getAll(Object... keys) {
        // UUID Key guard
        UUID playerUUID = null;
        for(Object key : keys){
            if(key instanceof UUID) playerUUID = (UUID) key;
        }

        // Missing key guard
        if(playerUUID == null) return null;

        // Build query and fetch
        String sql = "select * from %s where player_uuid = ?";
        ResultSet rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString());

        if(rs == null) return null;

        List<TeleportAttempt> teleportAttempts = new ArrayList<>();
        try{
            while(rs.next()){
                Player player = Bukkit.getPlayer(playerUUID);
                Location location = new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z")
                );
                teleportAttempts.add(new TeleportAttempt(player, location));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().info(String.format("There was an issue retrieving teleport attempts for player %s", playerUUID));
            return null;
        }

        return teleportAttempts;
    }

    @Override
    public TeleportAttempt get(Object... keys) {
        Player player = null;
        for(Object key : keys){
            if(key instanceof Player) player = (Player) key;
        }

        if(player == null) return null;

        String sql = "select * from %s where player_uuid = ?";
        ResultSet rs = DatabaseUtil.fetch(this.conn, String.format(sql, TABLE_NAME), player.getUniqueId().toString());

        // Guard to check if we received a result set
        if (rs == null) return null;

        TeleportAttempt ta = null;
        try {
            // Guard to check if the result set has rows
            if (!rs.next()) {
                return null;
            }

            // Create the teleport attempt obj
            ta = new TeleportAttempt(
                    player,
                    new Location(
                            Bukkit.getWorld(UUID.fromString(rs.getString("world"))),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z")
                    )
            );

            // Check if player can still teleport
            Location teleportStart = ta.getLocation();
            Location currLocation = player.getLocation();

            // Skip cancel on move check
            if (!ConfigUtil.getConfig().getBoolean("cancelOnMove", true)) return ta;
            if (player.hasPermission("sh2.bypass-teleport-delay")) return ta;

            // Check if player has moved
            if (teleportStart.getX() != currLocation.getX() || teleportStart.getY() != currLocation.getY() || teleportStart.getZ() != currLocation.getZ())
                ta.setCanTeleport(false);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("There was an issue reading a teleportation attempt for player " + player.getUniqueId());
        }

        return ta;
    }

    @Override
    public boolean save(Object object) {
        if(!(object instanceof TeleportAttempt)) return false;

        TeleportAttempt ta = (TeleportAttempt) object;
        String sql = "insert into %s (player_uuid, world, x, y, z) VALUES (?, ?, ?, ?, ?);";
        return DatabaseUtil.execute(
                this.conn,
                String.format(sql, TABLE_NAME),
                ta.getPlayer().getUniqueId().toString(),
                Objects.requireNonNull(ta.getLocation().getWorld()).getUID().toString(),
                ta.getLocation().getX(),
                ta.getLocation().getY(),
                ta.getLocation().getZ()
        );
    }

    @Override
    public boolean delete(Object object) {
        if(!(object instanceof UUID)) return false;

        UUID playerUUID = (UUID) object;
        String sql = "delete from %s where player_uuid = ?";
        return DatabaseUtil.execute(this.conn, String.format(sql, TABLE_NAME), playerUUID.toString());
    }
}

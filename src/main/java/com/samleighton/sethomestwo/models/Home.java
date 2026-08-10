package com.samleighton.sethomestwo.models;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.Dao;
import com.samleighton.sethomestwo.dao.TeleportAttemptsDao;
import com.samleighton.sethomestwo.enums.UserError;
import com.samleighton.sethomestwo.enums.UserInfo;
import com.samleighton.sethomestwo.enums.UserSuccess;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.TeleportSafetyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Home implements Serializable {

    private String uuidBelongingTo;
    private String material;
    private String name;
    private String description;
    private String world;
    private String dimension;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;
    private boolean canTeleport = true;

    public Home(String playerUUID, String material, Location location, String name, String description, String dimension) {
        setUUIDBelongingTo(playerUUID);
        setMaterial(material);
        setWorld(Objects.requireNonNull(location.getWorld()).getUID().toString());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setPitch(location.getPitch());
        setYaw(location.getYaw());
        setName(name);
        setDescription(description);
        setDimension(dimension);
    }

    public String getUUIDBelongingTo() {
        return uuidBelongingTo;
    }

    public void setUUIDBelongingTo(String uuidBelongingTo) {
        this.uuidBelongingTo = uuidBelongingTo;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    /**
     * Bukkit world UUID
     *
     * @implNote Use UUID.fromString() to create UUID obj
     * @return String
     */
    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public Location asLocation() {
        return new Location(
                Bukkit.getWorld(UUID.fromString(getWorld())),
                getX(),
                getY(),
                getZ(),
                getYaw(),
                getPitch()
        );
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public boolean getCanTeleport() {
        return canTeleport;
    }

    public void setCanTeleport(boolean canTeleport) {
        this.canTeleport = canTeleport;
    }

    public void teleport(Player player) {
        // Home is blacklisted guard
        if(!this.getCanTeleport()) {
            ChatUtils.sendError(player, ConfigUtil.getConfig().getString("teleportToBlacklistedDimension", UserError.TELEPORT_IS_BLACKLISTED.getValue()));
            return;
        }

        Dao<TeleportAttempt> teleportAttemptsDao = new TeleportAttemptsDao();
        boolean isAlreadyTeleporting = teleportAttemptsDao.get(player) != null;

        // Guard to check if player is currently teleporting
        if (isAlreadyTeleporting) {
            ChatUtils.sendError(player, ConfigUtil.getConfig().getString("teleportedWhileTeleporting", UserError.ALREADY_TELEPORTING.getValue()));
            return;
        }

        // Track player teleport attempt
        teleportAttemptsDao.save(new TeleportAttempt(player, player.getLocation()));

        // Send player countdown title.
        Plugin plugin = SetHomesTwo.getPlugin(SetHomesTwo.class);
        AtomicInteger seconds = new AtomicInteger(ConfigUtil.getConfig().getInt("delay"));

        // Schedule repeating task for every second
        plugin.getServer().getScheduler().runTaskTimer(plugin, bukkitTask -> {
            // Guard to check if task has been cancelled.
            if(bukkitTask.isCancelled()) return;

            // Guard if the player has moved
            TeleportAttempt currAttempt = teleportAttemptsDao.get(player);
            if (currAttempt != null) {
                if (!currAttempt.canTeleport()) {
                    ChatUtils.sendError(player, ConfigUtil.getConfig().getString("movedWhileTeleporting", UserError.MOVED_WHILE_TELEPORTING.getValue()));
                    player.playSound(player, Sound.ENTITY_PLAYER_BIG_FALL, 5f, 5f);
                    teleportAttemptsDao.delete(player.getUniqueId());
                    player.resetTitle();
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                    bukkitTask.cancel();
                    return;
                }
            }

            // This logic repeats until the time has expired.
            if (seconds.get() > 0) {
                String title = ConfigUtil.getConfig().getString("teleportTitle", "Please stand still");
                String subtitle = ConfigUtil.getConfig().getString("teleportSubtitle", "You will be teleported in %d...");
                player.sendTitle(ChatColor.GOLD + title, String.format(subtitle, seconds.get()), 0, 999, 0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 999, 0 , true));
                player.playNote(player.getLocation(), Instrument.DIDGERIDOO, Note.sharp(2, Note.Tone.F));
                seconds.decrementAndGet();
                return;
            }

            bukkitTask.cancel();
            // This logic fires after total seconds have elapsed
            teleportAttemptsDao.delete(player.getUniqueId());

            // Resolve a safe destination before moving the player
            Location destination = this.asLocation();
            if (ConfigUtil.getConfig().getBoolean("teleportSafety", true)) {
                Location safeDestination = TeleportSafetyUtil.findSafeLocation(destination);
                if (safeDestination == null) {
                    ChatUtils.sendError(player, ConfigUtil.getConfig().getString("unsafeHome", UserError.UNSAFE_HOME.getValue()));
                    player.resetTitle();
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                    return;
                }
                if (safeDestination != destination) {
                    ChatUtils.sendInfo(player, ConfigUtil.getConfig().getString("movedToSafeSpot", UserInfo.MOVED_TO_SAFE_SPOT.getValue()));
                }
                destination = safeDestination;
            }

            player.teleport(destination);
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.resetTitle();
            player.playNote(player.getLocation(), Instrument.BELL, Note.sharp(2, Note.Tone.F));
            player.spawnParticle(Particle.PORTAL, player.getLocation(), 100);

            String teleportSuccess = ConfigUtil.getConfig().getString("teleportSuccess", UserSuccess.TELEPORTED.getValue());
            ChatUtils.sendSuccess(player, String.format(teleportSuccess, this.getName()));

        }, 0, 20L);
    }
}

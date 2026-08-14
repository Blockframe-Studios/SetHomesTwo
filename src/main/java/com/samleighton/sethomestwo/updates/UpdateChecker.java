package com.samleighton.sethomestwo.updates;

import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Asks a {@link ReleaseSource} whether a newer release exists and, if so, tells
 * the console and every player holding {@link #NOTIFY_PERMISSION}.
 */
public class UpdateChecker {

    public static final String NOTIFY_PERMISSION = "sh2.update-notify";

    public static final String RELEASES_URL = "https://github.com/Blockframe-Studios/SetHomesTwo/releases";

    /** Keeps the request off the boot path. */
    public static final long STARTUP_DELAY_TICKS = 100L;

    private final Plugin plugin;
    private final String currentVersion;
    private final ReleaseSource source;

    /** Written by the async check, read from the main thread. */
    private volatile String availableVersion;

    public UpdateChecker(Plugin plugin, String currentVersion, ReleaseSource source) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
        this.source = source;
    }

    /**
     * Runs the check off the main thread, once the server has settled.
     */
    public void checkLater() {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            // Read when the task fires, so a reload can still turn the check off.
            if (!plugin.getConfig().getBoolean("checkForUpdates", true)) return;
            checkNow();
        }, STARTUP_DELAY_TICKS);
    }

    /**
     * Contacts the release source and records the result. Never throws; a failed
     * check is logged at info level and otherwise ignored.
     */
    public void checkNow() {
        String latestTag;
        try {
            latestTag = source.latestTag();
        } catch (Exception e) {
            if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("Could not check for SetHomesTwo updates: " + e.getMessage());
            return;
        }

        if (!VersionCompare.isNewer(latestTag, currentVersion)) return;

        availableVersion = latestTag;
        Bukkit.getLogger().info(
                "SetHomesTwo " + latestTag + " is available (running " + currentVersion + "). " + RELEASES_URL
        );

        notifyPlayersAlreadyOnline();
    }

    /**
     * Covers players who joined before the check landed - the join listener has
     * already run for them. Hops to the main thread because {@link #checkNow()}
     * runs off it.
     */
    private void notifyPlayersAlreadyOnline() {
        if (!plugin.isEnabled()) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                notifyIfUpdateAvailable(player);
            }
        });
    }

    /**
     * The newer release tag, or null when up to date or no check has completed.
     */
    public String getAvailableVersion() {
        return availableVersion;
    }

    public void notifyIfUpdateAvailable(Player player) {
        String version = availableVersion;
        if (version == null) return;
        if (!player.hasPermission(NOTIFY_PERMISSION)) return;

        ChatUtils.sendInfo(player, "SetHomesTwo " + version + " is available (you are on " + currentVersion + ")");
        ChatUtils.sendInfo(player, "Download: " + RELEASES_URL);
    }
}

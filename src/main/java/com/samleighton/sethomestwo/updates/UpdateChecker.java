package com.samleighton.sethomestwo.updates;

import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.utils.ChatUtils;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Asks a {@link ReleaseSource} whether a newer release exists and, if so, tells
 * the console once at startup and each permitted player as they join.
 */
public class UpdateChecker {

    public static final String NOTIFY_PERMISSION = "sh2.update-notify";

    public static final String RELEASES_URL = "https://github.com/Blockframe-Studios/SetHomesTwo/releases";

    /**
     * The first check waits for the server to finish starting rather than adding
     * network latency to boot.
     */
    public static final long STARTUP_DELAY_TICKS = 100L;

    private final Plugin plugin;
    private final String currentVersion;
    private final ReleaseSource source;

    /**
     * The newer tag once one has been seen, otherwise null. Written from the
     * async check and read from the main thread, hence volatile.
     */
    private volatile String availableVersion;

    public UpdateChecker(Plugin plugin, String currentVersion, ReleaseSource source) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
        this.source = source;
    }

    /**
     * Runs the check off the main thread once the server has settled. A network
     * call on the main thread stalls every player on the server for its duration.
     */
    public void checkLater() {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            // Read when the task fires rather than when it is scheduled, so a
            // reload that turns the setting off is honoured before any request
            // leaves the server.
            if (!plugin.getConfig().getBoolean("checkForUpdates", true)) return;
            checkNow();
        }, STARTUP_DELAY_TICKS);
    }

    /**
     * Contacts the release source and records the result. Never throws: a server
     * with no outbound network is not a broken server, so a failure here is
     * reported at info level and otherwise ignored.
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
    }

    /**
     * The newer release tag, or null when up to date or no check has completed.
     */
    public String getAvailableVersion() {
        return availableVersion;
    }

    /**
     * Sends the notice to a player who is allowed to see it. Players who cannot
     * replace the jar are deliberately left alone.
     */
    public void notifyIfUpdateAvailable(Player player) {
        String version = availableVersion;
        if (version == null) return;
        if (!player.hasPermission(NOTIFY_PERMISSION)) return;

        ChatUtils.sendInfo(player, "SetHomesTwo " + version + " is available (you are on " + currentVersion + ")");
        ChatUtils.sendInfo(player, "Download: " + RELEASES_URL);
    }
}

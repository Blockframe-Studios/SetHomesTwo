package com.samleighton.sethomestwo.updates;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Remembers which release this server has already been told about and when, so
 * a pending update is announced on a cadence instead of on every join.
 *
 * <p>State is server-wide rather than per player, and is deliberately kept out
 * of config.yml, which server owners edit. This is plugin bookkeeping.
 */
public class UpdateNotificationLog {

    public static final String FILE_NAME = "update-notifications.yml";

    private static final String VERSION_KEY = "lastNotifiedVersion";
    private static final String TIMESTAMP_KEY = "lastNotifiedAt";

    private final File file;
    private final YamlConfiguration state;

    public UpdateNotificationLog(File file) {
        this.file = file;
        // Returns an empty configuration when the file is absent, which is the
        // correct starting point for a server that has never been notified.
        this.state = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * @return the release last announced, or null when none ever was
     */
    public String lastVersion() {
        return state.getString(VERSION_KEY);
    }

    /**
     * @return when that announcement happened, in epoch millis, or 0 if never
     */
    public long lastNotifiedAt() {
        return state.getLong(TIMESTAMP_KEY);
    }

    public void record(String version, long epochMillis) {
        state.set(VERSION_KEY, version);
        state.set(TIMESTAMP_KEY, epochMillis);

        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            state.save(file);
        } catch (IOException e) {
            // Worth a line, but not worth failing a join over: a lost write
            // only costs one extra notice next boot.
            Bukkit.getLogger().info("Could not save SetHomesTwo update notification state: " + e.getMessage());
        }
    }
}

package com.samleighton.sethomestwo;

import com.samleighton.sethomestwo.commands.*;
import com.samleighton.sethomestwo.connections.ConnectionManager;
import com.samleighton.sethomestwo.dao.TeleportAttemptsDao;
import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.events.PlayerJoin;
import com.samleighton.sethomestwo.events.PlayerLeave;
import com.samleighton.sethomestwo.events.PlayerMoveWhileTeleporting;
import com.samleighton.sethomestwo.events.RightClickHomeItem;
import com.samleighton.sethomestwo.gui.GuiSession;
import com.samleighton.sethomestwo.gui.HomesGui;
import com.samleighton.sethomestwo.models.TeleportAttempt;
import com.samleighton.sethomestwo.updates.GitHubReleaseSource;
import com.samleighton.sethomestwo.updates.UpdateChecker;
import com.samleighton.sethomestwo.tabcompleters.BlacklistTabCompleter;
import com.samleighton.sethomestwo.tabcompleters.HomesTabCompleter;
import com.samleighton.sethomestwo.tabcompleters.ImportSourcesTabCompleter;
import com.samleighton.sethomestwo.tabcompleters.MaterialsTabCompleter;
import com.samleighton.sethomestwo.tabcompleters.PlayerHomesTabCompleter;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import com.samleighton.sethomestwo.utils.DatabaseUtil;
import com.samleighton.sethomestwo.utils.PermissionOverrides;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SetHomesTwo extends JavaPlugin {
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final Map<UUID, GuiSession> guiSessionMap = new HashMap<>();
    private UpdateChecker updateChecker;

    /**
     * Looked up by name because JavaPlugin.getPlugin(SetHomesTwo.class) needs the
     * class loaded by Bukkit's PluginClassLoader, which never holds under MockBukkit.
     */
    public static SetHomesTwo instance() {
        return (SetHomesTwo) Bukkit.getPluginManager().getPlugin("SetHomesTwo");
    }

    @Override
    public void onEnable() {
        // Create the directories for the plugin
        createDirectories();

        // Create config
        initConfig();

        // After the config exists, before commands are registered.
        PermissionOverrides.apply();

        // Built before the listeners: the join listener is handed this instance.
        updateChecker = new UpdateChecker(
                this,
                getDescription().getVersion(),
                new GitHubReleaseSource(getDescription().getVersion())
        );
        updateChecker.checkLater();

        // Plugin startup logic
        registerCommands();
        registerEventListeners();

        // Load online players into gui map just in case this was a reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            guiSessionMap.put(player.getUniqueId(), new GuiSession(new HomesGui(player)));
        }

        // Init database connections
        boolean success = connectionManager.createConnection("homes", "homes");
        if (success) {
            if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("Homes database connection was successful.");

            boolean createdTables = DatabaseUtil.initTables(connectionManager.getConnection("homes"));
            if (createdTables && ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("Table initialization was successfully executed.");

            // Any teleport attempt present at startup is stale (crash or offline player
            // at shutdown) and would permanently block that player's teleports.
            boolean clearedAttempts = DatabaseUtil.execute(
                    connectionManager.getConnection("homes"),
                    "delete from player_teleport_attempts;"
            );
            if (!clearedAttempts)
                Bukkit.getLogger().severe("Could not clear stale teleport attempts on startup.");

        } else {
            Bukkit.getLogger().severe("Could not create database connection!");
        }
    }

    @Override
    public void onDisable() {
        // Clear teleport attempts for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Attempt to find a teleport attempt for the player
            TeleportAttemptsDao tad = new TeleportAttemptsDao();
            TeleportAttempt currAttempt = tad.get(player);

            // Skip if no teleport attempt was found
            if (currAttempt == null) continue;

            // If the player has a teleport attempt, clear it
            tad.delete(player.getUniqueId());
            player.resetTitle();
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }

        // Close database connections
        connectionManager.closeConnections();

        if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
            Bukkit.getLogger().info("All database connections closed.");
    }

    /**
     * Initialize the default values for the config
     */
    public void initConfig() {
        File outputConfig = new File(getDataFolder(), "config.yml");
        if (outputConfig.exists()) return;

        try {
            if (!outputConfig.createNewFile()) return;
        } catch (IOException e) {
            Bukkit.getLogger().info("Could not create output config file for initializing default config.yml!");
        }

        InputStream is = this.getResource("default-config.yml");
        if (is == null) return;

        try {
            byte[] buffer = new byte[is.available()];
            int bytes = is.read(buffer);

            if (bytes > 0 && ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("Writing default config file...");

            Files.write(outputConfig.toPath(), buffer);
        } catch (IOException e) {
            Bukkit.getLogger().info("There was an issue copying data to config.yml");
        }
    }

    /**
     * Register all commands for the plugin.
     */
    public void registerCommands() {
        Objects.requireNonNull(this.getCommand("give-homes-item")).setExecutor(new GiveHomesItem());

        PluginCommand listHomes = Objects.requireNonNull(this.getCommand("list-homes"));
        listHomes.setExecutor(new ListHomes());

        PluginCommand homes = Objects.requireNonNull(this.getCommand("homes"));
        homes.setExecutor(new OpenHomesGui(this));

        PluginCommand goHome = Objects.requireNonNull(this.getCommand("go-home"));
        goHome.setExecutor(new GoHome());
        goHome.setTabCompleter(new HomesTabCompleter());

        PluginCommand createHome = Objects.requireNonNull(this.getCommand("create-home"));
        createHome.setExecutor(new CreateHome());
        createHome.setTabCompleter(new MaterialsTabCompleter());

        PluginCommand deleteHome = Objects.requireNonNull(this.getCommand("delete-home"));
        deleteHome.setExecutor(new DeleteHome());
        deleteHome.setTabCompleter(new HomesTabCompleter());

        PluginCommand blacklist = Objects.requireNonNull(this.getCommand("blacklist"));
        blacklist.setExecutor(new Blacklist());
        blacklist.setTabCompleter(new BlacklistTabCompleter());

        PluginCommand getPlayerHomes = Objects.requireNonNull(this.getCommand("get-player-homes"));
        getPlayerHomes.setExecutor(new GetPlayerHomes(this));

        PluginCommand setMaxHomes = Objects.requireNonNull(this.getCommand("set-max-homes"));
        setMaxHomes.setExecutor(new SetMaxHomes(this));

        PluginCommand importHomes = Objects.requireNonNull(this.getCommand("import-homes"));
        importHomes.setExecutor(new ImportHomes());
        importHomes.setTabCompleter(new ImportSourcesTabCompleter());

        PluginCommand moveHome = Objects.requireNonNull(this.getCommand("move-home"));
        moveHome.setExecutor(new MoveHome());
        moveHome.setTabCompleter(new HomesTabCompleter());

        PluginCommand goPlayerHome = Objects.requireNonNull(this.getCommand("go-player-home"));
        goPlayerHome.setExecutor(new GoPlayerHome());
        goPlayerHome.setTabCompleter(new PlayerHomesTabCompleter());

        PluginCommand deletePlayerHome = Objects.requireNonNull(this.getCommand("delete-player-home"));
        deletePlayerHome.setExecutor(new DeletePlayerHome());
        deletePlayerHome.setTabCompleter(new PlayerHomesTabCompleter());

        PluginCommand movePlayerHome = Objects.requireNonNull(this.getCommand("move-player-home"));
        movePlayerHome.setExecutor(new MovePlayerHome());
        movePlayerHome.setTabCompleter(new PlayerHomesTabCompleter());
    }

    /**
     * Register all event listeners for the plugin
     */
    public void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoin(this, updateChecker), this);
        getServer().getPluginManager().registerEvents(new PlayerLeave(this), this);
        getServer().getPluginManager().registerEvents(new RightClickHomeItem(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveWhileTeleporting(), this);
    }

    /**
     * Creates the directories necessary for plugin functionality
     */
    public void createDirectories() {
        // Create the plugin directory
        if (!getDataFolder().exists()) {
            boolean success = getDataFolder().mkdir();
            if (!success)
                Bukkit.getLogger().severe("Could not create plugin folder.");
        }

        File databasesDir = new File(getDataFolder().getAbsolutePath() + "/database");
        if (!databasesDir.exists()) {
            boolean success = databasesDir.mkdir();
            if (!success)
                Bukkit.getLogger().severe("Could not create database folder.");
        }
    }

    /**
     * Retrieves the plugin's connection manager.
     *
     * @return ConnectionManager
     */
    public ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }

    public Map<UUID, GuiSession> getGuiSessionMap() {
        return this.guiSessionMap;
    }
}

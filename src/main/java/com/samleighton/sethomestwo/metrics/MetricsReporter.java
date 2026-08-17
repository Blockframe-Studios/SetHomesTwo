package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.SetHomesTwo;
import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.enums.DebugLevel;
import com.samleighton.sethomestwo.utils.ConfigUtil;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedBarChart;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Sends anonymous usage counts to bStats. The only class that touches
 * org.bstats; everything else increments {@link UsageCounters}.
 */
public class MetricsReporter {

    /** bStats service id for SetHomesTwo, from bstats.org. The reporter stays off while this is 0. */
    public static final int PLUGIN_ID = 33420;

    /** Keeps bStats off the boot path, same as the update check. */
    public static final long STARTUP_DELAY_TICKS = 100L;

    /**
     * Developer switch: start the server with -Dsethomestwo.metrics.disabled=true
     * and nothing is reported. For test and end-to-end servers, so their traffic
     * never reaches the public dashboard. Deliberately not a config.yml key.
     */
    public static final String DISABLE_PROPERTY = "sethomestwo.metrics.disabled";

    private final SetHomesTwo plugin;
    private final int pluginId;
    private final BooleanSupplier enabled;
    private final Function<UsageCounters, AutoCloseable> factory;

    private AutoCloseable running;

    public MetricsReporter(SetHomesTwo plugin) {
        this(plugin,
                PLUGIN_ID,
                () -> shouldReport(plugin.getDataFolder().getParentFile()),
                counters -> new BStatsHandle(plugin, counters));
    }

    /**
     * Whether a live server should report: not switched off by the developer
     * property, and not disabled through bStats' own server-wide config.
     */
    static boolean shouldReport(File pluginsDir) {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            Bukkit.getLogger().info("SetHomesTwo metrics are off: " + DISABLE_PROPERTY + " is set.");
            return false;
        }
        return bStatsEnabledGlobally(pluginsDir);
    }

    /**
     * bStats' server-wide switch, plugins/bStats/config.yml. Metrics has no
     * per-plugin toggle, so this is the one owner-facing opt-out and it is
     * honoured before bStats is built at all. A missing file means enabled, as
     * bStats itself treats it.
     */
    static boolean bStatsEnabledGlobally(File pluginsDir) {
        File config = new File(new File(pluginsDir, "bStats"), "config.yml");
        if (!config.isFile()) return true;
        return YamlConfiguration.loadConfiguration(config).getBoolean("enabled", true);
    }

    MetricsReporter(SetHomesTwo plugin, int pluginId, BooleanSupplier enabled, Function<UsageCounters, AutoCloseable> factory) {
        this.plugin = plugin;
        this.pluginId = pluginId;
        this.enabled = enabled;
        this.factory = factory;
    }

    /**
     * Schedules the start. The bStats switch is read when the task fires, not
     * when scheduled.
     */
    public void startLater() {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::startNow, STARTUP_DELAY_TICKS);
    }

    private void startNow() {
        if (running != null || pluginId <= 0 || !enabled.getAsBoolean()) return;
        try {
            running = factory.apply(plugin.getUsageCounters());
        } catch (Throwable t) {
            if (ConfigUtil.getDebugLevel().equals(DebugLevel.INFO))
                Bukkit.getLogger().info("Could not start bStats metrics: " + t.getMessage());
        }
    }

    public void shutdown() {
        if (running == null) return;
        try {
            running.close();
        } catch (Exception ignored) {
            // Nothing sensible to do at shutdown.
        }
        running = null;
    }

    public boolean isRunning() {
        return running != null;
    }

    /**
     * bStats chart id for one command's line chart: `command_` plus the name
     * with hyphens as underscores, so `go-home` reports under `command_go_home`.
     * Each id has to be registered on the plugin's bStats page.
     */
    static String commandChartId(String commandName) {
        return "command_" + commandName.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * bStats chart id for one alias's line chart, `alias_` plus the alias with
     * hyphens as underscores. Each id has to be registered on the plugin's
     * bStats page.
     */
    static String aliasChartId(String alias) {
        return "alias_" + alias.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * Every alias declared in plugin.yml, lower-cased, in declaration order.
     */
    static List<String> declaredAliases(SetHomesTwo plugin) {
        List<String> aliases = new ArrayList<>();
        for (Map<String, Object> command : plugin.getDescription().getCommands().values()) {
            Object declared = command.get("aliases");
            if (declared instanceof String) {
                aliases.add(((String) declared).toLowerCase(Locale.ROOT));
            } else if (declared instanceof Iterable<?>) {
                for (Object alias : (Iterable<?>) declared) aliases.add(String.valueOf(alias).toLowerCase(Locale.ROOT));
            }
        }
        return aliases;
    }

    /**
     * The real bStats wiring. Constructed only on a live server; the relocation
     * check inside Metrics throws under an unshaded classpath.
     */
    private static final class BStatsHandle implements AutoCloseable {
        private final Metrics metrics;

        BStatsHandle(SetHomesTwo plugin, UsageCounters counters) {
            metrics = new Metrics(plugin, PLUGIN_ID);

            // bStats keeps history (and time filters) only for line charts, and a
            // line chart carries one number, so each usage family gets a bar chart
            // for the ranking of the last window plus line charts for trends.
            // WindowShare hands every chart the same drained window whatever order
            // bStats calls them in, so nothing is read twice.
            WindowShare share = new WindowShare(counters);

            for (String command : plugin.getDescription().getCommands().keySet()) {
                metrics.addCustomChart(new SingleLineChart(commandChartId(command),
                        () -> share.count(UsageCounters.Family.COMMAND, command)));
            }
            metrics.addCustomChart(new SingleLineChart("commands_total", () -> share.total(UsageCounters.Family.COMMAND)));

            for (String alias : declaredAliases(plugin)) {
                metrics.addCustomChart(new SingleLineChart(aliasChartId(alias),
                        () -> share.count(UsageCounters.Family.ALIAS, alias)));
            }

            metrics.addCustomChart(new AdvancedBarChart("gui_action_usage", () -> share.bars(UsageCounters.Family.GUI_ACTION)));
            metrics.addCustomChart(new SingleLineChart("gui_actions_total", () -> share.total(UsageCounters.Family.GUI_ACTION)));

            metrics.addCustomChart(new AdvancedBarChart("errors", () -> share.bars(UsageCounters.Family.ERROR)));
            metrics.addCustomChart(new SingleLineChart("errors_total", () -> share.total(UsageCounters.Family.ERROR)));


            metrics.addCustomChart(new AdvancedPie("teleport_outcome", () -> counters.snapshotAndReset(UsageCounters.Family.TELEPORT_OUTCOME)));

            metrics.addCustomChart(new SimplePie("max_homes_enabled", () -> String.valueOf(config().getBoolean("maxHomeEnabled", false))));
            metrics.addCustomChart(new SimplePie("max_homes_type", () -> {
                if (!config().getBoolean("maxHomeEnabled", false)) return "off";
                String type = config().getString("maxHomesType");
                return type == null ? "groups" : type.toLowerCase(Locale.ROOT);
            }));
            metrics.addCustomChart(new SimplePie("cancel_on_move", () -> String.valueOf(config().getBoolean("cancelOnMove", true))));
            metrics.addCustomChart(new SimplePie("teleport_safety", () -> String.valueOf(config().getBoolean("teleportSafety", true))));
            metrics.addCustomChart(new SimplePie("teleport_delay", () -> Buckets.delay(config().getInt("delay", 3))));
            metrics.addCustomChart(new SimplePie("open_home_item", () -> defaultOrCustom("openHomeItem", "compass")));
            metrics.addCustomChart(new SimplePie("default_home_item", () -> defaultOrCustom("defaultHomeItem", "white_wool")));
            metrics.addCustomChart(new SimplePie("luckperms_installed", () -> String.valueOf(Bukkit.getPluginManager().getPlugin("LuckPerms") != null)));
            metrics.addCustomChart(new SimplePie("homes_per_server", () -> Buckets.homesPerServer(new HomesDao().countAll())));
            metrics.addCustomChart(new SimplePie("homes_per_player", () -> {
                HomesDao dao = new HomesDao();
                return Buckets.homesPerPlayer(dao.countAll(), dao.countPlayersWithHomes());
            }));
        }

        private static FileConfiguration config() {
            return ConfigUtil.getConfig();
        }

        private static String defaultOrCustom(String key, String defaultValue) {
            String value = config().getString(key, defaultValue);
            return value != null && value.equalsIgnoreCase(defaultValue) ? "default" : "custom";
        }

        @Override
        public void close() {
            metrics.shutdown();
        }
    }
}

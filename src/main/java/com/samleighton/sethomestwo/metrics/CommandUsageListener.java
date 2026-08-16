package com.samleighton.sethomestwo.metrics;

import com.samleighton.sethomestwo.SetHomesTwo;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

/**
 * Counts every SetHomesTwo command typed by a player or the console, by
 * canonical name and by the label actually typed. Commands owned by other
 * plugins are ignored. Never cancels or alters the event.
 */
public class CommandUsageListener implements Listener {

    private final SetHomesTwo plugin;

    public CommandUsageListener(SetHomesTwo plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        count(event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsoleCommand(ServerCommandEvent event) {
        count(event.getCommand());
    }

    /**
     * Counts the command on the given line if it belongs to this plugin.
     * Swallows runtime failures: a metrics failure must never abort a command.
     */
    void count(String commandLine) {
        try {
            String label = firstToken(commandLine);
            if (label == null) return;

            PluginCommand command = Bukkit.getPluginCommand(label);
            if (command == null || command.getPlugin() != plugin) return;

            String namespace = plugin.getName().toLowerCase(Locale.ROOT) + ":";
            String typed = label.startsWith(namespace) ? label.substring(namespace.length()) : label;

            UsageCounters counters = plugin.getUsageCounters();
            counters.increment(UsageCounters.Family.COMMAND, command.getName());
            counters.increment(UsageCounters.Family.ALIAS, typed);
        } catch (RuntimeException ignored) {
            // Counting is best effort.
        }
    }

    private static String firstToken(String commandLine) {
        if (commandLine == null) return null;
        String trimmed = commandLine.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        if (trimmed.isEmpty()) return null;
        int space = trimmed.indexOf(' ');
        String label = space < 0 ? trimmed : trimmed.substring(0, space);
        return label.toLowerCase(Locale.ROOT);
    }
}

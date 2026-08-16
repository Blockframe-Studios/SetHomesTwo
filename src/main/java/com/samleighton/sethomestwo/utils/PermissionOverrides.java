package com.samleighton.sethomestwo.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.Locale;

/**
 * Applies the config.yml permissions block over the defaults declared in
 * plugin.yml. Only a node's default changes, so an explicit grant or deny in a
 * permissions plugin still wins.
 */
public final class PermissionOverrides {

    private PermissionOverrides() {
    }

    public static void apply() {
        ConfigurationSection section = ConfigUtil.getConfig().getConfigurationSection("permissions");
        if (section == null) return;

        PluginManager pluginManager = Bukkit.getPluginManager();

        // Deep keys, because Bukkit splits a dotted key such as sh2.import-homes
        // into nested sections. The intermediate sections are not nodes.
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) continue;

            // Bukkit lowercases a node name when looking it up but not when
            // storing it as a bundle child, so detachFromBundles only matches
            // the canonical spelling. The config value still reads by raw key.
            String node = key.toLowerCase(Locale.ROOT);

            Permission permission = pluginManager.getPermission(node);
            if (permission == null) {
                Bukkit.getLogger().warning(String.format(
                        "SetHomesTwo: ignoring unknown permission node '%s' in config.yml.", key));
                continue;
            }

            String raw = section.getString(key);
            PermissionDefault parsed = raw == null ? null : PermissionDefault.getByName(raw);
            if (parsed == null) {
                Bukkit.getLogger().warning(String.format(
                        "SetHomesTwo: ignoring permission '%s', value '%s' is not one of true, false, op, not-op.",
                        key, raw));
                continue;
            }

            // Detach before the no-op check below. A node whose default already
            // matches still needs freeing from its bundle, or the bundle keeps
            // granting the very thing the admin just asked to take away.
            if (parsed != PermissionDefault.TRUE) detachFromBundles(pluginManager, node, parsed);

            PermissionDefault previous = permission.getDefault();
            if (previous == parsed) continue;

            permission.setDefault(parsed);
            pluginManager.recalculatePermissionDefaults(permission);

            Bukkit.getLogger().info(String.format(
                    "SetHomesTwo: permission default changed, %s %s to %s", node, previous, parsed));

            if ("sh2.import-homes".equals(node) && parsed != PermissionDefault.OP) {
                Bukkit.getLogger().warning(
                        "SetHomesTwo: sh2.import-homes is no longer operator only. "
                                + "/import-homes <source> confirm writes homes for every player on the server.");
            }
        }
    }

    /**
     * Drop a node from every bundle that lists it as a child, so the node's own
     * default governs again.
     *
     * Bukkit writes the children of a default-granted parent straight into a
     * player's effective permission map, and hasPermission reads that map before
     * falling back to the node's default. Lowering the default alone therefore
     * denies nothing while a bundle still grants the node.
     */
    private static void detachFromBundles(PluginManager pluginManager, String node, PermissionDefault applied) {
        for (Permission bundle : pluginManager.getPermissions()) {
            if (bundle.getChildren().remove(node) == null) continue;

            pluginManager.recalculatePermissionDefaults(bundle);
            Bukkit.getLogger().info(String.format(
                    "SetHomesTwo: %s is now %s, and was removed from the %s bundle so that applies.",
                    node, applied, bundle.getName()));
        }
    }
}

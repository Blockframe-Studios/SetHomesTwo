package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.models.Home;
import com.samleighton.sethomestwo.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerHomesTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Only online players are offered, though the commands themselves also
        // accept an offline player who has saved homes.
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            StringUtil.copyPartialMatches(args[0], names, completions);
            return completions;
        }

        if (args.length == 2) {
            String uuid = ServerUtil.getPlayerUUID(args[0]);
            if (uuid == null) return completions;

            List<String> homeNames = new ArrayList<>();
            for (Home home : new HomesDao(true).getAll(UUID.fromString(uuid))) {
                homeNames.add(home.getName());
            }

            StringUtil.copyPartialMatches(args[1], homeNames, completions);
        }

        return completions;
    }
}

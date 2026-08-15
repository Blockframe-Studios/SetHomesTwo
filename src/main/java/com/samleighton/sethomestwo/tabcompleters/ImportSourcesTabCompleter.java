package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.commands.ImportHomes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Completions for /import-homes. Returns an empty list rather than null past the
 * arguments it knows, because Bukkit falls back to suggesting online player
 * names whenever a completer returns null, which is meaningless here.
 */
public class ImportSourcesTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], ImportHomes.SOURCES.keySet(), completions);
        }

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], List.of("confirm"), completions);
        }

        return completions;
    }
}

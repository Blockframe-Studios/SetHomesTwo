package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.dao.BlacklistDao;
import com.samleighton.sethomestwo.utils.ServerUtil;
import com.samleighton.sethomestwo.utils.TabCompletions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlacklistTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Only the canonical name takes a subcommand argument; the old names go
        // straight to world names, matching how the executor reads them.
        boolean canonical = "blacklist".equalsIgnoreCase(label);

        if (canonical && args.length == 1) {
            completions.addAll(TabCompletions.matching(args[0], Arrays.asList("add", "remove", "list")));
            return completions;
        }

        boolean removing = "remove-from-blacklist".equalsIgnoreCase(label)
                || (canonical && args.length > 0 && "remove".equalsIgnoreCase(args[0]));
        List<String> source = removing ? new BlacklistDao().getAll() : ServerUtil.getValidDimensions();

        String lastArg = args.length == 0 ? "" : args[args.length - 1];
        completions.addAll(TabCompletions.matching(lastArg, source));
        return completions;
    }
}

package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.utils.ServerUtil;
import com.samleighton.sethomestwo.utils.TabCompletions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DimensionTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        for(String arg : args){
            completions.addAll(TabCompletions.matching(arg, ServerUtil.getValidDimensions()));
        }

        return completions;
    }
}

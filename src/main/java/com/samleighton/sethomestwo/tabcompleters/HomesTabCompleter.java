package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.dao.HomesDao;
import com.samleighton.sethomestwo.utils.HomesUtil;
import com.samleighton.sethomestwo.utils.TabCompletions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HomesTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        final List<String> completions = new ArrayList<>();

        // Guard to check if command sender is player
        if (!(commandSender instanceof Player)) return new ArrayList<>();

        Player player = (Player) commandSender;
        List<String> homeNames = HomesUtil.getPlayerHomesNameOnly(new HomesDao(), player.getUniqueId());

        completions.addAll(TabCompletions.matching(args[0], homeNames));
        return completions;
    }
}

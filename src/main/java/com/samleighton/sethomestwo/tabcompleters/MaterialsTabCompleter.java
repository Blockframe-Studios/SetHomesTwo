package com.samleighton.sethomestwo.tabcompleters;

import com.samleighton.sethomestwo.utils.TabCompletions;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaterialsTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> validMaterials = new ArrayList<String>(){
            {
                add("d");
                add("default");
            }
        };
        List<String> completions = new ArrayList<>();

        if(args.length != 2) return completions;

        // Add all valid materials
        Material[] allMaterials = Material.values();
        for(Material mat : allMaterials){
            if(!mat.isItem()) continue;
            // Every non-legacy material is keyed minecraft:<name>, so this equals
            // getKey() without touching the deprecated accessor.
            validMaterials.add("minecraft:" + mat.name().toLowerCase(Locale.ROOT));
        }

        completions.addAll(TabCompletions.matching(args[1], validMaterials));
        return completions;
    }
}

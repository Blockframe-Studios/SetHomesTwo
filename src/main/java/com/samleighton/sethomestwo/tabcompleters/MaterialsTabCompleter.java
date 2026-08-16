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
            validMaterials.add(mat.getKey().toString().toLowerCase());
        }

        completions.addAll(TabCompletions.matching(args[1], validMaterials));
        return completions;
    }
}

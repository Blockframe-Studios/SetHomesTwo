package com.samleighton.sethomestwo.commands;

import com.samleighton.sethomestwo.importers.HomesImporter;
import com.samleighton.sethomestwo.importers.ImportReport;
import com.samleighton.sethomestwo.importers.SetHomesV1Importer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImportHomes implements CommandExecutor {

    // Task 6 adds the EssentialsX importer here.
    public static final Map<String, HomesImporter> SOURCES = new LinkedHashMap<>();

    static {
        HomesImporter setHomesV1 = new SetHomesV1Importer();
        SOURCES.put(setHomesV1.sourceName(), setHomesV1);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        // Console and players may both run this; permission handled by plugin.yml (default op)
        if (args.length < 1 || !SOURCES.containsKey(args[0].toLowerCase())) {
            commandSender.sendMessage(String.format("Usage: /import-homes <%s> [confirm]", String.join("|", SOURCES.keySet())));
            return true;
        }

        HomesImporter importer = SOURCES.get(args[0].toLowerCase());
        boolean dryRun = args.length < 2 || !args[1].equalsIgnoreCase("confirm");

        ImportReport report = importer.run(dryRun);
        commandSender.sendMessage(report.summary(dryRun));
        for (String warning : report.warnings) {
            commandSender.sendMessage("Warning: " + warning);
        }
        if (dryRun && report.imported > 0) {
            commandSender.sendMessage(String.format("Dry run only. Run '/import-homes %s confirm' to apply.", importer.sourceName()));
        }
        return true;
    }
}

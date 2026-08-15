package com.samleighton.sethomestwo.importers;

import java.util.ArrayList;
import java.util.List;

public class ImportReport {
    public int imported = 0;
    public int skippedExisting = 0;
    public int skippedWorldMissing = 0;
    public int failed = 0;
    public int blacklistImported = 0;
    public int blacklistSkippedExisting = 0;
    public int namesResolved = 0;
    public final List<String> warnings = new ArrayList<>();
    public final List<String> configNotes = new ArrayList<>();

    public String summary(boolean dryRun) {
        String verb = dryRun ? "Would import" : "Imported";
        String base = String.format(
                "%s %d homes (%d skipped: name exists, %d skipped: world missing, %d failed).",
                verb, imported, skippedExisting, skippedWorldMissing, failed
        );
        if (namesResolved > 0) {
            base += String.format(" %d player name(s) resolved from the server's cache.", namesResolved);
        }
        return base;
    }

    public boolean hasBlacklistActivity() {
        return blacklistImported > 0 || blacklistSkippedExisting > 0;
    }

    public String blacklistSummary(boolean dryRun) {
        String verb = dryRun ? "Would add" : "Added";
        return String.format(
                "%s %d world(s) to the blacklist (%d already blacklisted).",
                verb, blacklistImported, blacklistSkippedExisting
        );
    }
}

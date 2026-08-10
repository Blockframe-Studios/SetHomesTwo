package com.samleighton.sethomestwo.importers;

import java.util.ArrayList;
import java.util.List;

public class ImportReport {
    public int imported = 0;
    public int skippedExisting = 0;
    public int skippedWorldMissing = 0;
    public int failed = 0;
    public final List<String> warnings = new ArrayList<>();

    public String summary(boolean dryRun) {
        String verb = dryRun ? "Would import" : "Imported";
        return String.format(
                "%s %d homes (%d skipped: name exists, %d skipped: world missing, %d failed).",
                verb, imported, skippedExisting, skippedWorldMissing, failed
        );
    }
}

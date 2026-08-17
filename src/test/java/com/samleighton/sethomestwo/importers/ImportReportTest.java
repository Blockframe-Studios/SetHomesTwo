package com.samleighton.sethomestwo.importers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportReportTest {

    @Test
    void theSummaryTellsARenameApartFromAHomeThatWasAlreadyImported() {
        ImportReport report = new ImportReport();
        report.imported = 11;
        report.renamed = 1;
        report.skippedExisting = 3;

        String summary = report.summary(false);

        assertTrue(summary.contains("1 renamed"), summary);
        assertTrue(summary.contains("3 skipped: already imported"), summary);
    }

    @Test
    void theSummaryKeepsItsShapeOnADryRun() {
        ImportReport report = new ImportReport();
        report.imported = 11;
        report.renamed = 1;

        String summary = report.summary(true);

        assertTrue(summary.startsWith("Would import 11 homes"), summary);
        assertTrue(summary.contains("1 renamed"), summary);
    }
}

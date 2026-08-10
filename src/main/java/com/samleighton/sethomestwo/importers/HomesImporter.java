package com.samleighton.sethomestwo.importers;

public interface HomesImporter {

    /** The name used as the command argument, e.g. "sethomes". */
    String sourceName();

    /** Scan the source and, unless dryRun, write homes. Never overwrites existing homes. */
    ImportReport run(boolean dryRun);
}

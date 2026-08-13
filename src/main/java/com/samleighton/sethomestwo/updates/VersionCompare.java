package com.samleighton.sethomestwo.updates;

/**
 * Compares dotted numeric version strings, e.g. the "1.2.0" in plugin.yml
 * against the "v1.3.0" tag name on a GitHub release.
 */
public final class VersionCompare {

    private VersionCompare() {
    }

    /**
     * Whether candidate names a strictly newer release than current.
     * <p>
     * Anything that is not purely numeric segments - a pre-release suffix, a
     * codename, an empty or null string - is treated as "not newer" so a stray
     * tag cannot push an update notice to every server running the plugin.
     *
     * @param candidate the version offered by the release feed
     * @param current   the version this plugin is running
     * @return true only when both parse and candidate is the higher of the two
     */
    public static boolean isNewer(String candidate, String current) {
        int[] offered = parse(candidate);
        int[] running = parse(current);
        if (offered == null || running == null) return false;

        int segments = Math.max(offered.length, running.length);
        for (int i = 0; i < segments; i++) {
            // A version that runs out of segments is padded with zeroes, so
            // "1.2" and "1.2.0" compare equal.
            int a = i < offered.length ? offered[i] : 0;
            int b = i < running.length ? running[i] : 0;
            if (a != b) return a > b;
        }

        return false;
    }

    /**
     * Splits a version into its numeric segments, tolerating a leading "v".
     *
     * @return the segments, or null when the string is not a plain numeric version
     */
    private static int[] parse(String version) {
        if (version == null) return null;

        String trimmed = version.trim();
        if (trimmed.regionMatches(true, 0, "v", 0, 1)) trimmed = trimmed.substring(1);
        if (trimmed.isEmpty()) return null;

        String[] parts = trimmed.split("[.]");
        int[] segments = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                segments[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return segments;
    }
}

package com.samleighton.sethomestwo.updates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCompareTest {

    @Test
    void higherPatchIsNewer() {
        assertTrue(VersionCompare.isNewer("1.2.1", "1.2.0"));
    }

    @Test
    void higherMinorIsNewer() {
        assertTrue(VersionCompare.isNewer("1.3.0", "1.2.0"));
    }

    @Test
    void higherMajorIsNewer() {
        assertTrue(VersionCompare.isNewer("2.0.0", "1.9.9"));
    }

    @Test
    void identicalVersionIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.2.0", "1.2.0"));
    }

    @Test
    void lowerVersionIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.1.0", "1.2.0"));
    }

    @Test
    void segmentsCompareNumericallyNotLexically() {
        assertTrue(VersionCompare.isNewer("1.10.0", "1.9.0"));
    }

    @Test
    void lexicallyLargerButNumericallyOlderIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.9.0", "1.10.0"));
    }

    @Test
    void leadingVOnTheReleaseTagIsIgnored() {
        assertTrue(VersionCompare.isNewer("v1.3.0", "1.2.0"));
    }

    @Test
    void leadingVOnTheRunningVersionIsIgnored() {
        assertFalse(VersionCompare.isNewer("v1.2.0", "v1.2.0"));
    }

    @Test
    void missingTrailingSegmentsCountAsZero() {
        assertFalse(VersionCompare.isNewer("1.2", "1.2.0"));
    }

    @Test
    void shorterVersionCanStillBeNewer() {
        assertTrue(VersionCompare.isNewer("1.3", "1.2.9"));
    }

    @Test
    void extraTrailingSegmentMakesItNewer() {
        assertTrue(VersionCompare.isNewer("1.2.0.1", "1.2.0"));
    }

    @Test
    void preReleaseSuffixIsNotAnnounced() {
        assertFalse(VersionCompare.isNewer("1.3.0-SNAPSHOT", "1.2.0"));
    }

    @Test
    void unparseableTagIsNotNewer() {
        assertFalse(VersionCompare.isNewer("nightly", "1.2.0"));
    }

    @Test
    void nullTagIsNotNewer() {
        assertFalse(VersionCompare.isNewer(null, "1.2.0"));
    }

    @Test
    void emptyTagIsNotNewer() {
        assertFalse(VersionCompare.isNewer("", "1.2.0"));
    }

    @Test
    void nullRunningVersionIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.3.0", null));
    }

    @Test
    void unparseableRunningVersionIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.3.0", "dev"));
    }
}

package com.samleighton.sethomestwo.updates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GitHubReleaseSourceTest {

    @Test
    void readsTheTagFromAReleaseBody() {
        String body = "{\"html_url\":\"https://github.com/x/y/releases/tag/v1.2.0\","
                + "\"tag_name\":\"v1.2.0\",\"name\":\"SetHomesTwo V1.2.0\"}";

        assertEquals("v1.2.0", GitHubReleaseSource.tagFrom(body));
    }

    @Test
    void surroundingWhitespaceIsTrimmed() {
        assertEquals("v1.2.0", GitHubReleaseSource.tagFrom("{\"tag_name\":\"  v1.2.0  \"}"));
    }

    @Test
    void rateLimitResponseHasNoTag() {
        String body = "{\"message\":\"API rate limit exceeded\",\"documentation_url\":\"https://docs.github.com\"}";

        assertNull(GitHubReleaseSource.tagFrom(body));
    }

    @Test
    void nullTagValueIsNotATag() {
        assertNull(GitHubReleaseSource.tagFrom("{\"tag_name\":null}"));
    }

    @Test
    void emptyTagValueIsNotATag() {
        assertNull(GitHubReleaseSource.tagFrom("{\"tag_name\":\"   \"}"));
    }

    @Test
    void nonObjectBodyHasNoTag() {
        assertNull(GitHubReleaseSource.tagFrom("[]"));
    }

    @Test
    void malformedBodyHasNoTag() {
        assertNull(GitHubReleaseSource.tagFrom("<html>502 Bad Gateway</html>"));
    }

    @Test
    void emptyBodyHasNoTag() {
        assertNull(GitHubReleaseSource.tagFrom(""));
    }

    @Test
    void nullBodyHasNoTag() {
        assertNull(GitHubReleaseSource.tagFrom(null));
    }
}

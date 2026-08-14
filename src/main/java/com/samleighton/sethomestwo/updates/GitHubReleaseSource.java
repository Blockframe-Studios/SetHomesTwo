package com.samleighton.sethomestwo.updates;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Reads the newest published release tag from the GitHub releases API.
 */
public class GitHubReleaseSource implements ReleaseSource {

    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/Blockframe-Studios/SetHomesTwo/releases/latest";

    private static final int TIMEOUT_MS = 5000;

    private final String userAgent;

    public GitHubReleaseSource(String pluginVersion) {
        this.userAgent = "SetHomesTwo/" + pluginVersion;
    }

    @Override
    public String latestTag() throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) URI.create(LATEST_RELEASE_URL).toURL().openConnection();

        connection.setRequestMethod("GET");
        // GitHub answers 403 to any request that does not identify itself.
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        try (InputStream stream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            return tagFrom(body.toString());
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Pulls tag_name out of a releases API response.
     *
     * @return the tag, or null when the response does not carry one
     */
    static String tagFrom(String body) {
        if (body == null) return null;

        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return null;

            JsonElement tag = root.getAsJsonObject().get("tag_name");
            if (tag == null || !tag.isJsonPrimitive()) return null;

            String value = tag.getAsString().trim();
            return value.isEmpty() ? null : value;
        } catch (RuntimeException e) {
            return null;
        }
    }
}

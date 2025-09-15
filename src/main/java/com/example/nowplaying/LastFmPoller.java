package com.example.nowplaying;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically polls Last.fm for the currently playing track of a user and updates NowPlayingService.
 */
public final class LastFmPoller {
    private static final Logger LOGGER = LoggerFactory.getLogger("modid");
    private static ScheduledExecutorService scheduler;
    private static volatile String currentUsername;
    private static volatile String currentApiKey;
    private static volatile java.util.function.Supplier<String> usernameSupplier;

    private LastFmPoller() {}

    public static synchronized void start(String apiKey, String username, Duration period) {
        Objects.requireNonNull(username, "username");
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warn("LASTFM polling disabled: missing API key.");
            return;
        }
        currentUsername = username;
        currentApiKey = apiKey;
        usernameSupplier = () -> currentUsername;
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lastfm-poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(LastFmPoller::pollCurrent, 2, Math.max(5, period.toSeconds()), TimeUnit.SECONDS);
        LOGGER.info("Last.fm polling started for user '{}'", username);
    }

    public static void updateUsername(String username) {
        if (username == null || username.isBlank()) return;
        currentUsername = username;
    }

    public static void updateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return;
        currentApiKey = apiKey;
    }

    private static void pollCurrent() {
        try {
            String apiKey = currentApiKey;
            String username = usernameSupplier != null ? usernameSupplier.get() : currentUsername;
            if (apiKey == null || apiKey.isBlank() || username == null || username.isBlank()) return;
            String url = "https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user="
                + urlEncode(username) + "&api_key=" + urlEncode(apiKey) + "&format=json&limit=1";
            String json = httpGet(url, 5000);
            Track track = parseFirstTrack(json);
            if (track != null && (track.nowPlaying || !track.title.isBlank())) {
                NowPlayingService.update(track.title, track.artist, "Last.fm");
            }
        } catch (Exception e) {
            // Keep silent to avoid spamming logs; print occasionally
        }
    }

    private static String httpGet(String urlStr, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    // Very small JSON extractor for the fields we need
    private static Track parseFirstTrack(String json) {
        if (json == null || json.isEmpty()) return null;
        int trackIdx = json.indexOf("\"track\":");
        if (trackIdx < 0) return null;
        int firstObj = json.indexOf('{', trackIdx);
        if (firstObj < 0) return null;
        int endObj = json.indexOf("}", firstObj);
        // We'll scan within a reasonable window
        int windowEnd = Math.min(json.length(), firstObj + 2000);
        String chunk = json.substring(firstObj, windowEnd);

        String title = extractValue(chunk, "\"name\":\"");
        String artist = extractNestedArtist(chunk);
        boolean nowPlaying = chunk.contains("\"nowplaying\":\"true\"");
        if (title == null) title = "";
        if (artist == null) artist = "";
        if (title.isBlank() && artist.isBlank()) return null;
        return new Track(title, artist, nowPlaying);
    }

    private static String extractNestedArtist(String json) {
        int artistIdx = json.indexOf("\"artist\":");
        if (artistIdx < 0) return null;
        int textIdx = json.indexOf("\"#text\":\"", artistIdx);
        if (textIdx < 0) return null;
        int start = textIdx + "\"#text\":\"".length();
        return readJSONString(json, start);
    }

    private static String extractValue(String json, String keyPattern) {
        int idx = json.indexOf(keyPattern);
        if (idx < 0) return null;
        int start = idx + keyPattern.length();
        return readJSONString(json, start);
    }

    private static String readJSONString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private record Track(String title, String artist, boolean nowPlaying) {}
}



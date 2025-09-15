package com.example.nowplaying;

import java.time.Instant;
import java.util.Objects;

/**
 * Holds the latest now-playing info pushed from an external source (e.g., browser extension).
 */
public final class NowPlayingService {
    private static volatile String currentTitle = "";
    private static volatile String currentArtist = "";
    private static volatile String currentSource = "";
    private static volatile Instant lastUpdatedAt = Instant.EPOCH;

    private NowPlayingService() {}

    public static synchronized void update(String title, String artist, String source) {
        currentTitle = nonNull(title);
        currentArtist = nonNull(artist);
        currentSource = nonNull(source);
        lastUpdatedAt = Instant.now();
    }

    public static synchronized void clear() {
        currentTitle = "";
        currentArtist = "";
        currentSource = "";
        lastUpdatedAt = Instant.EPOCH;
    }

    public static String getFormattedNowPlaying() {
        String title = currentTitle;
        String artist = currentArtist;
        String source = currentSource;
        if (title.isEmpty() && artist.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Сейчас играет: ");
        if (!artist.isEmpty()) {
            sb.append(artist).append(" — ");
        }
        sb.append(title);
        if (!source.isEmpty()) {
            sb.append(" [").append(source).append("]");
        }
        return sb.toString();
    }

    public static Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public static String getCurrentTitle() {
        return currentTitle;
    }

    public static String getCurrentArtist() {
        return currentArtist;
    }

    public static String getCurrentSource() {
        return currentSource;
    }

    private static String nonNull(String s) {
        return Objects.requireNonNullElse(s, "").trim();
    }
}



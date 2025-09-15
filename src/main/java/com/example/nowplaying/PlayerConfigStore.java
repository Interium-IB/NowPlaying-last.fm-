package com.example.nowplaying;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public final class PlayerConfigStore {
    private PlayerConfigStore() {}

    public static String getLastfmUsername(UUID playerUuid, String fallback) {
        if (playerUuid == null) return fallback;
        Path path = playerFile(playerUuid);
        if (!Files.exists(path)) return fallback;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            String u = p.getProperty("lastfm_username", fallback);
            return u == null ? fallback : u.trim();
        } catch (IOException e) {
            return fallback;
        }
    }

    public static void setLastfmUsername(UUID playerUuid, String username) {
        if (playerUuid == null) return;
        Properties p = new Properties();
        p.setProperty("lastfm_username", username == null ? "" : username.trim());
        Path path = playerFile(playerUuid);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                p.store(out, "Per-player now playing settings");
            }
        } catch (IOException ignored) {}
    }

    private static Path playerFile(UUID playerUuid) {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("youtube-music-nowplaying").resolve("players");
        return dir.resolve(playerUuid.toString() + ".properties");
    }
}



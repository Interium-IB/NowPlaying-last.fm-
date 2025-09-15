package com.example.nowplaying;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class NowPlayingConfig {
    private static final String FILE_NAME = "youtube-music-nowplaying.properties";

    private String lastfmUsername = "upsetsummer";
    private String lastfmApiKey = ""; // optional; can come from env/props

    public String getLastfmUsername() {
        return lastfmUsername;
    }

    public void setLastfmUsername(String lastfmUsername) {
        this.lastfmUsername = lastfmUsername == null ? "" : lastfmUsername.trim();
    }

    public String getLastfmApiKey() {
        return lastfmApiKey;
    }

    public void setLastfmApiKey(String lastfmApiKey) {
        this.lastfmApiKey = lastfmApiKey == null ? "" : lastfmApiKey.trim();
    }

    public static NowPlayingConfig load() {
        NowPlayingConfig cfg = new NowPlayingConfig();
        Path path = configPath();
        if (Files.exists(path)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
                cfg.lastfmUsername = p.getProperty("lastfm_username", cfg.lastfmUsername).trim();
                cfg.lastfmApiKey = p.getProperty("lastfm_api_key", cfg.lastfmApiKey).trim();
            } catch (IOException ignored) {}
        }
        return cfg;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("lastfm_username", String.valueOf(lastfmUsername));
        p.setProperty("lastfm_api_key", String.valueOf(lastfmApiKey));
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                p.store(out, "YouTube Music Now Playing config");
            }
        } catch (IOException ignored) {}
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}



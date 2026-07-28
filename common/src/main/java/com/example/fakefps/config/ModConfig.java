package com.example.fakefps.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String mode = "static";
    private int fakeFps = 120;
    private int minFps = 30;
    private int maxFps = 300;

    private transient final Random random = new Random();
    private transient Path configPath;
    private transient int cachedRandomFps = 60;
    private transient long lastRandomUpdate; // nanoTime of last random refresh

    private ModConfig() {}

    // ── Getters ──

    public boolean isEnabled()        { return enabled; }
    public String getMode()           { return mode; }
    public int getFakeFps()           { return fakeFps; }
    public int getMinFps()            { return minFps; }
    public int getMaxFps()            { return maxFps; }

    // ── Setters ──

    public void setEnabled(boolean v) { this.enabled = v; }
    public void setMode(String v)     { this.mode = v; }
    public void setFakeFps(int v)     { this.fakeFps = Math.max(1, v); }
    public void setMinFps(int v)      { this.minFps = Math.max(1, v); }
    public void setMaxFps(int v)      { this.maxFps = Math.max(1, v); }

    public int getCurrentFps() {
        return switch (mode) {
            case "random" -> {
                long now = System.nanoTime();
                if (now - lastRandomUpdate >= 1_000_000_000L) {
                    int range = maxFps - minFps;
                    cachedRandomFps = range > 0 ? minFps + random.nextInt(range + 1) : minFps;
                    lastRandomUpdate = now;
                }
                yield cachedRandomFps;
            }
            default      -> fakeFps;
        };
    }

    // ── Persistence ──

    public void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[FakeFPS] Failed to save config: " + e.getMessage());
        }
    }

    public static ModConfig load(Path configDir) {
        Path configPath = configDir.resolve("fakefps.json");
        ModConfig config;
        if (Files.exists(configPath)) {
            try {
                config = GSON.fromJson(Files.readString(configPath), ModConfig.class);
            } catch (IOException e) {
                System.err.println("[FakeFPS] Failed to load config: " + e.getMessage());
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
        }
        config.configPath = configPath;
        config.save();
        return config;
    }
}

package com.example.fakefps.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.fakefps.FakeFPSMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fakefps.json");

    private boolean enabled = true;
    private String mode = "static"; // static, random
    private int fakeFps = 120;
    private int minFps = 30;
    private int maxFps = 300;

    private transient final Random random = new Random();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getFakeFps() {
        return fakeFps;
    }

    public void setFakeFps(int fakeFps) {
        this.fakeFps = Math.max(1, fakeFps);
    }

    public int getMinFps() {
        return minFps;
    }

    public void setMinFps(int minFps) {
        this.minFps = Math.max(1, minFps);
    }

    public int getMaxFps() {
        return maxFps;
    }

    public void setMaxFps(int maxFps) {
        this.maxFps = Math.max(1, maxFps);
    }

    public int getCurrentFps() {
        return switch (mode) {
            case "random" -> minFps + random.nextInt(maxFps - minFps + 1);
            default -> fakeFps;
        };
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            FakeFPSMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                return GSON.fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
            } catch (IOException e) {
                FakeFPSMod.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }
}

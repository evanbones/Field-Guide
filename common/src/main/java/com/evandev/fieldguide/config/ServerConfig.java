package com.evandev.fieldguide.config;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("fieldguide-server.json").toFile();

    private static ServerConfig INSTANCE;
    private static ServerConfig SYNCED_INSTANCE;

    public boolean disableScanning = false;
    public boolean disableLootDisplay = false;
    public boolean disableBiomeDisplay = false;
    public boolean disableEditingDescriptions = false;
    public boolean disableEditingNames = false;
    public boolean disableVariants = false;
    public boolean keepSilhouetteWhenUnlocked = false;
    public boolean unlockAllVariants = false;
    public boolean enableFieldGuideItem = false;
    public boolean enableLensItem = false;
    public boolean requireItemToOpen = false;
    public boolean enableCopyingPages = true;
    public boolean disableObtainUnlocks = false;

    public boolean hideTabsUntilUnlocked = false;

    public boolean enableSpyglassScanning = true;
    public double spyglassScanDistance = 64.0D;
    public boolean enableSpyglassDiscovery = false;

    public boolean enableNakedEyeScanning = false;
    public double nakedEyeScanDistance = 10.0D;

    public boolean enableNakedEyeDiscovery = false;
    public double nakedEyeDiscoveryDistance = 8.0D;

    public boolean enableFieldGuideScanning = false;
    public double fieldGuideScanDistance = 4.0D;

    public double lensScanDistance = 6.0D;
    public boolean enableLensDiscovery = true;
    public double lensDiscoveryDistance = 24.0D;

    public boolean showUndiscoveredNames = false;
    public boolean hideUndiscoveredFromSearch = false;

    public double scanSpeed = 1.0D;
    public double minScanHitboxSize = 1.0D;
    public boolean grantXpOnScan = true;
    public int xpAmountOnScan = 5;

    public boolean enableReliableRemover = true;
    public boolean exposureUnlockViaPhotograph = true;

    public List<String> globalScanCommands = new ArrayList<>();
    public Map<String, List<String>> categoryScanCommands = new HashMap<>();
    public Map<String, List<String>> entryScanCommands = new HashMap<>();

    public static ServerConfig get() {
        if (SYNCED_INSTANCE != null) {
            return SYNCED_INSTANCE;
        }
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void setSyncedConfig(ServerConfig config) {
        SYNCED_INSTANCE = config;
    }

    public static void resetSyncedConfig() {
        SYNCED_INSTANCE = null;
    }

    public static ServerConfig getLocal() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ServerConfig.class);
                if (INSTANCE.categoryScanCommands == null) INSTANCE.categoryScanCommands = new HashMap<>();
                if (INSTANCE.entryScanCommands == null) INSTANCE.entryScanCommands = new HashMap<>();
                if (INSTANCE.globalScanCommands == null) INSTANCE.globalScanCommands = new ArrayList<>();
            } catch (Exception e) {
                Constants.LOG.error("Failed to load fieldguide-server.json", e);
                INSTANCE = new ServerConfig();
            }
        } else {
            INSTANCE = new ServerConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save fieldguide-server.json", e);
        }
    }

    public static ServerConfig fromJson(String json) {
        return GSON.fromJson(json, ServerConfig.class);
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}

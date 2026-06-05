package com.evandev.fieldguide.config;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File OLD_CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("fieldguide.json").toFile();

    public static void load() {
        if (OLD_CONFIG_FILE.exists()) {
            migrate();
        }
        ClientConfig.load();
        ServerConfig.load();
    }

    private static void migrate() {
        Constants.LOG.info("Migrating old fieldguide.json to new split configuration...");
        try (FileReader reader = new FileReader(OLD_CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            ClientConfig client = ClientConfig.get();
            ServerConfig server = ServerConfig.get();

            // Client settings
            if (json.has("showPauseMenuButton")) client.showPauseMenuButton = json.get("showPauseMenuButton").getAsBoolean();
            if (json.has("pauseButtonXOffset")) client.pauseButtonXOffset = json.get("pauseButtonXOffset").getAsInt();
            if (json.has("pauseButtonYOffset")) client.pauseButtonYOffset = json.get("pauseButtonYOffset").getAsInt();
            if (json.has("showInventoryButton")) client.showInventoryButton = json.get("showInventoryButton").getAsBoolean();
            if (json.has("inventoryButtonXOffset")) client.inventoryButtonXOffset = json.get("inventoryButtonXOffset").getAsInt();
            if (json.has("inventoryButtonYOffset")) client.inventoryButtonYOffset = json.get("inventoryButtonYOffset").getAsInt();
            if (json.has("defaultScreen")) client.defaultScreen = json.get("defaultScreen").getAsString();
            if (json.has("scanIconYOffset")) client.scanIconYOffset = json.get("scanIconYOffset").getAsInt();
            if (json.has("scanIconXOffset")) client.scanIconXOffset = json.get("scanIconXOffset").getAsInt();
            if (json.has("showScanIcon")) client.showScanIcon = json.get("showScanIcon").getAsBoolean();
            if (json.has("playScanningSound")) client.playScanningSound = json.get("playScanningSound").getAsBoolean();
            if (json.has("scanOverlayColor")) client.scanOverlayColor = json.get("scanOverlayColor").getAsString();
            if (json.has("scanOverlayAlpha")) client.scanOverlayAlpha = json.get("scanOverlayAlpha").getAsDouble();
            if (json.has("textColor")) client.textColor = json.get("textColor").getAsString();
            if (json.has("textTitleColor")) client.textTitleColor = json.get("textTitleColor").getAsString();
            if (json.has("textMutedColor")) client.textMutedColor = json.get("textMutedColor").getAsString();
            if (json.has("textCursorColor")) client.textCursorColor = json.get("textCursorColor").getAsString();
            if (json.has("pageNumberColor")) client.pageNumberColor = json.get("pageNumberColor").getAsString();
            if (json.has("listSilhouetteColor")) client.listSilhouetteColor = json.get("listSilhouetteColor").getAsString();
            if (json.has("listSilhouetteAlpha")) client.listSilhouetteAlpha = json.get("listSilhouetteAlpha").getAsDouble();
            if (json.has("listUnlockedSilhouetteColor")) client.listUnlockedSilhouetteColor = json.get("listUnlockedSilhouetteColor").getAsString();
            if (json.has("listUnlockedSilhouetteAlpha")) client.listUnlockedSilhouetteAlpha = json.get("listUnlockedSilhouetteAlpha").getAsDouble();
            if (json.has("detailsSilhouetteColor")) client.detailsSilhouetteColor = json.get("detailsSilhouetteColor").getAsString();
            if (json.has("detailsSilhouetteAlpha")) client.detailsSilhouetteAlpha = json.get("detailsSilhouetteAlpha").getAsDouble();
            if (json.has("detailsUnlockedSilhouetteColor")) client.detailsUnlockedSilhouetteColor = json.get("detailsUnlockedSilhouetteColor").getAsString();
            if (json.has("detailsUnlockedSilhouetteAlpha")) client.detailsUnlockedSilhouetteAlpha = json.get("detailsUnlockedSilhouetteAlpha").getAsDouble();
            if (json.has("useRealWorldDate")) client.useRealWorldDate = json.get("useRealWorldDate").getAsBoolean();
            if (json.has("showToasts")) client.showToasts = json.get("showToasts").getAsBoolean();
            if (json.has("exposureAddPhotographButton")) client.exposureAddPhotographButton = json.get("exposureAddPhotographButton").getAsBoolean();
            if (json.has("exposureShowPhotographsInGrid")) client.exposureShowPhotographsInGrid = json.get("exposureShowPhotographsInGrid").getAsBoolean();

            // Server settings
            if (json.has("disableScanning")) server.disableScanning = json.get("disableScanning").getAsBoolean();
            if (json.has("disableLootDisplay")) server.disableLootDisplay = json.get("disableLootDisplay").getAsBoolean();
            if (json.has("disableBiomeDisplay")) server.disableBiomeDisplay = json.get("disableBiomeDisplay").getAsBoolean();
            if (json.has("disableEditingDescriptions")) server.disableEditingDescriptions = json.get("disableEditingDescriptions").getAsBoolean();
            if (json.has("disableEditingNames")) server.disableEditingNames = json.get("disableEditingNames").getAsBoolean();
            if (json.has("keepSilhouetteWhenUnlocked")) server.keepSilhouetteWhenUnlocked = json.get("keepSilhouetteWhenUnlocked").getAsBoolean();
            if (json.has("unlockAllVariants")) server.unlockAllVariants = json.get("unlockAllVariants").getAsBoolean();
            if (json.has("enableFieldGuideItem")) server.enableFieldGuideItem = json.get("enableFieldGuideItem").getAsBoolean();
            if (json.has("enableLensItem")) server.enableLensItem = json.get("enableLensItem").getAsBoolean();
            if (json.has("enableCopyingPages")) server.enableCopyingPages = json.get("enableCopyingPages").getAsBoolean();
            if (json.has("hideTabsUntilUnlocked")) server.hideTabsUntilUnlocked = json.get("hideTabsUntilUnlocked").getAsBoolean();
            if (json.has("enableSpyglassScanning")) server.enableSpyglassScanning = json.get("enableSpyglassScanning").getAsBoolean();
            if (json.has("spyglassScanDistance")) server.spyglassScanDistance = json.get("spyglassScanDistance").getAsDouble();
            if (json.has("enableSpyglassDiscovery")) server.enableSpyglassDiscovery = json.get("enableSpyglassDiscovery").getAsBoolean();
            if (json.has("enableNakedEyeScanning")) server.enableNakedEyeScanning = json.get("enableNakedEyeScanning").getAsBoolean();
            if (json.has("nakedEyeScanDistance")) server.nakedEyeScanDistance = json.get("nakedEyeScanDistance").getAsDouble();
            if (json.has("enableNakedEyeDiscovery")) server.enableNakedEyeDiscovery = json.get("enableNakedEyeDiscovery").getAsBoolean();
            if (json.has("nakedEyeDiscoveryDistance")) server.nakedEyeDiscoveryDistance = json.get("nakedEyeDiscoveryDistance").getAsDouble();
            if (json.has("enableFieldGuideScanning")) server.enableFieldGuideScanning = json.get("enableFieldGuideScanning").getAsBoolean();
            if (json.has("fieldGuideScanDistance")) server.fieldGuideScanDistance = json.get("fieldGuideScanDistance").getAsDouble();
            if (json.has("lensScanDistance")) server.lensScanDistance = json.get("lensScanDistance").getAsDouble();
            if (json.has("enableLensDiscovery")) server.enableLensDiscovery = json.get("enableLensDiscovery").getAsBoolean();
            if (json.has("lensDiscoveryDistance")) server.lensDiscoveryDistance = json.get("lensDiscoveryDistance").getAsDouble();
            if (json.has("showUndiscoveredNames")) server.showUndiscoveredNames = json.get("showUndiscoveredNames").getAsBoolean();
            if (json.has("hideUndiscoveredFromSearch")) server.hideUndiscoveredFromSearch = json.get("hideUndiscoveredFromSearch").getAsBoolean();
            if (json.has("scanSpeed")) server.scanSpeed = json.get("scanSpeed").getAsDouble();
            if (json.has("grantXpOnScan")) server.grantXpOnScan = json.get("grantXpOnScan").getAsBoolean();
            if (json.has("xpAmountOnScan")) server.xpAmountOnScan = json.get("xpAmountOnScan").getAsInt();
            if (json.has("enableReliableRemover")) server.enableReliableRemover = json.get("enableReliableRemover").getAsBoolean();
            if (json.has("exposureUnlockViaPhotograph")) server.exposureUnlockViaPhotograph = json.get("exposureUnlockViaPhotograph").getAsBoolean();

            if (json.has("globalScanCommands")) server.globalScanCommands = GSON.fromJson(json.get("globalScanCommands"), Constants.LIST_STRING_TYPE);
            if (json.has("categoryScanCommands")) server.categoryScanCommands = GSON.fromJson(json.get("categoryScanCommands"), Constants.MAP_STRING_LIST_STRING_TYPE);
            if (json.has("entryScanCommands")) server.entryScanCommands = GSON.fromJson(json.get("entryScanCommands"), Constants.MAP_STRING_LIST_STRING_TYPE);

            ClientConfig.save();
            ServerConfig.save();

            reader.close();
            Files.move(OLD_CONFIG_FILE.toPath(), OLD_CONFIG_FILE.toPath().resolveSibling("fieldguide.json.old"));
            Constants.LOG.info("Migration successful.");
        } catch (Exception e) {
            Constants.LOG.error("Failed to migrate fieldguide.json", e);
        }
    }
}

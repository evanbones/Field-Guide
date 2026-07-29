package com.evandev.fieldguide.config;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModConfigScreen {

    public static Screen createScreen(Screen parent) {
        ClientConfig clientConfig = ClientConfig.get();
        ServerConfig serverConfig = ServerConfig.getLocal();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("title.fieldguide.config"))
                .save(() -> {
                    ClientConfig.save();
                    ServerConfig.save();
                    ClientFieldGuideManager.clearCache();

                    if (Minecraft.getInstance().hasSingleplayerServer() && Minecraft.getInstance().getSingleplayerServer() != null) {
                        Minecraft.getInstance().getSingleplayerServer().execute(() -> ServerFieldGuideManager.getInstance().reload(Minecraft.getInstance().getSingleplayerServer()));
                    }
                });

        // General (Gameplay)
        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.translatable("category.fieldguide.general"))
                .option(createBoolOption("enable_field_guide_item", false, () -> serverConfig.enableFieldGuideItem, val -> serverConfig.enableFieldGuideItem = val))
                .option(createBoolOption("require_item_to_open", false, () -> serverConfig.requireItemToOpen, val -> serverConfig.requireItemToOpen = val))
                .option(createBoolOption("enable_copying_pages", true, () -> serverConfig.enableCopyingPages, val -> serverConfig.enableCopyingPages = val))
                .option(createStringOption("default_screen", "last_opened_screen", () -> clientConfig.defaultScreen, val -> clientConfig.defaultScreen = val))
                .option(createBoolOption("hide_tabs_until_unlocked", false, () -> serverConfig.hideTabsUntilUnlocked, val -> serverConfig.hideTabsUntilUnlocked = val))
                .option(createBoolOption("show_undiscovered_names", false, () -> serverConfig.showUndiscoveredNames, val -> serverConfig.showUndiscoveredNames = val))
                .option(createBoolOption("hide_undiscovered_from_search", false, () -> serverConfig.hideUndiscoveredFromSearch, val -> serverConfig.hideUndiscoveredFromSearch = val))
                .option(createBoolOption("disable_scanning", false, () -> serverConfig.disableScanning, val -> serverConfig.disableScanning = val))
                .option(createBoolOption("enable_spyglass_scanning", true, () -> serverConfig.enableSpyglassScanning, val -> serverConfig.enableSpyglassScanning = val))
                .option(createBoolOption("enable_naked_eye_scanning", false, () -> serverConfig.enableNakedEyeScanning, val -> serverConfig.enableNakedEyeScanning = val))
                .option(createDoubleOption("scan_speed", 1.0D, 0.1D, 10.0D, () -> serverConfig.scanSpeed, val -> serverConfig.scanSpeed = val))
                .option(createDoubleOption("spyglass_scan_distance", 64.0D, 1.0D, 256.0D, () -> serverConfig.spyglassScanDistance, val -> serverConfig.spyglassScanDistance = val))
                .option(createDoubleOption("naked_eye_scan_distance", 10.0D, 1.0D, 256.0D, () -> serverConfig.nakedEyeScanDistance, val -> serverConfig.nakedEyeScanDistance = val))
                .option(createBoolOption("grant_xp_on_scan", true, () -> serverConfig.grantXpOnScan, val -> serverConfig.grantXpOnScan = val))
                .option(createBoolOption("enable_reliable_remover", true, () -> serverConfig.enableReliableRemover, val -> serverConfig.enableReliableRemover = val))
                .option(createIntOption("xp_amount_on_scan", 5, 0, 1000, () -> serverConfig.xpAmountOnScan, val -> serverConfig.xpAmountOnScan = val))
                .option(createBoolOption("disable_loot_display", false, () -> serverConfig.disableLootDisplay, val -> serverConfig.disableLootDisplay = val))
                .option(createBoolOption("disable_biome_display", false, () -> serverConfig.disableBiomeDisplay, val -> serverConfig.disableBiomeDisplay = val))
                .option(createBoolOption("disable_variants", false, () -> serverConfig.disableVariants, val -> serverConfig.disableVariants = val))
                .option(createBoolOption("disable_editing_descriptions", false, () -> serverConfig.disableEditingDescriptions, val -> serverConfig.disableEditingDescriptions = val))
                .option(createBoolOption("disable_editing_names", false, () -> serverConfig.disableEditingNames, val -> serverConfig.disableEditingNames = val))
                .option(createBoolOption("show_toasts", true, () -> clientConfig.showToasts, val -> clientConfig.showToasts = val))
                .option(createBoolOption("keep_silhouette", false, () -> serverConfig.keepSilhouetteWhenUnlocked, val -> serverConfig.keepSilhouetteWhenUnlocked = val))
                .option(createBoolOption("unlock_variants", false, () -> serverConfig.unlockAllVariants, val -> serverConfig.unlockAllVariants = val));

        // Interface (Visuals)
        ConfigCategory.Builder interfaceCat = ConfigCategory.createBuilder()
                .name(Component.translatable("category.fieldguide.interface"))
                .option(createBoolOption("show_unlock_date", true, () -> clientConfig.showUnlockDate, val -> clientConfig.showUnlockDate = val))
                .option(createBoolOption("use_real_world_date", false, () -> clientConfig.useRealWorldDate, val -> clientConfig.useRealWorldDate = val))
                .option(createBoolOption("play_scanning_sound", true, () -> clientConfig.playScanningSound, val -> clientConfig.playScanningSound = val))
                .option(createBoolOption("play_unlock_sound", true, () -> clientConfig.playUnlockSound, val -> clientConfig.playUnlockSound = val))
                .option(createBoolOption("show_scan_icon", true, () -> clientConfig.showScanIcon, val -> clientConfig.showScanIcon = val))
                .option(createIntOption("scan_icon_y_offset", 2, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.scanIconYOffset, val -> clientConfig.scanIconYOffset = val))
                .option(createIntOption("scan_icon_x_offset", 30, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.scanIconXOffset, val -> clientConfig.scanIconXOffset = val))
                .option(createBoolOption("show_out_of_range_overlay", true, () -> clientConfig.showOutOfRangeOverlay, val -> clientConfig.showOutOfRangeOverlay = val))
                .option(createStringOption("scan_overlay_color", "#F9EED0", () -> clientConfig.scanOverlayColor, val -> clientConfig.scanOverlayColor = val))
                .option(createDoubleOption("scan_overlay_alpha", 0.5D, 0.0D, 1.0D, () -> clientConfig.scanOverlayAlpha, val -> clientConfig.scanOverlayAlpha = val))
                .option(createBoolOption("show_pause_button", true, () -> clientConfig.showPauseMenuButton, val -> clientConfig.showPauseMenuButton = val))
                .option(createIntOption("pause_button_x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.pauseButtonXOffset, val -> clientConfig.pauseButtonXOffset = val))
                .option(createIntOption("pause_button_y", 0, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.pauseButtonYOffset, val -> clientConfig.pauseButtonYOffset = val))
                .option(createBoolOption("show_inventory_button", true, () -> clientConfig.showInventoryButton, val -> clientConfig.showInventoryButton = val))
                .option(createIntOption("inventory_button_x", 126, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.inventoryButtonXOffset, val -> clientConfig.inventoryButtonXOffset = val))
                .option(createIntOption("inventory_button_y", 61, Integer.MIN_VALUE, Integer.MAX_VALUE, () -> clientConfig.inventoryButtonYOffset, val -> clientConfig.inventoryButtonYOffset = val))
                .option(createStringOption("text_color", "#8A5E3B", () -> clientConfig.textColor, val -> clientConfig.textColor = val))
                .option(createStringOption("text_title_color", "#704623", () -> clientConfig.textTitleColor, val -> clientConfig.textTitleColor = val))
                .option(createStringOption("text_muted_color", "#C7A875", () -> clientConfig.textMutedColor, val -> clientConfig.textMutedColor = val))
                .option(createStringOption("text_cursor_color", "#0xFF704623", () -> clientConfig.textCursorColor, val -> clientConfig.textCursorColor = val))
                .option(createStringOption("page_number_color", "#C7A875", () -> clientConfig.pageNumberColor, val -> clientConfig.pageNumberColor = val))
                .option(createStringOption("list_silhouette_color", "#DDC69B", () -> clientConfig.listSilhouetteColor, val -> clientConfig.listSilhouetteColor = val))
                .option(createDoubleOption("list_silhouette_alpha", 1.0D, 0.0D, 1.0D, () -> clientConfig.listSilhouetteAlpha, val -> clientConfig.listSilhouetteAlpha = val))
                .option(createStringOption("list_unlocked_silhouette_color", "#DDC69B", () -> clientConfig.listUnlockedSilhouetteColor, val -> clientConfig.listUnlockedSilhouetteColor = val))
                .option(createDoubleOption("list_unlocked_silhouette_alpha", 1.0D, 0.0D, 1.0D, () -> clientConfig.listUnlockedSilhouetteAlpha, val -> clientConfig.listUnlockedSilhouetteAlpha = val))
                .option(createStringOption("details_silhouette_color", "#DDC69B", () -> clientConfig.detailsSilhouetteColor, val -> clientConfig.detailsSilhouetteColor = val))
                .option(createDoubleOption("details_silhouette_alpha", 1.0D, 0.0D, 1.0D, () -> clientConfig.detailsSilhouetteAlpha, val -> clientConfig.detailsSilhouetteAlpha = val))
                .option(createStringOption("details_unlocked_silhouette_color", "#DDC69B", () -> clientConfig.detailsUnlockedSilhouetteColor, val -> clientConfig.detailsUnlockedSilhouetteColor = val))
                .option(createDoubleOption("details_unlocked_silhouette_alpha", 1.0D, 0.0D, 1.0D, () -> clientConfig.detailsUnlockedSilhouetteAlpha, val -> clientConfig.detailsUnlockedSilhouetteAlpha = val));

        // Commands
        ConfigCategory.Builder commandCat = ConfigCategory.createBuilder()
                .name(Component.translatable("category.fieldguide.commands"))
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("option.fieldguide.global_scan_commands"))
                        .description(OptionDescription.of(Component.translatable("option.fieldguide.global_scan_commands.tooltip")))
                        .binding(new ArrayList<>(), () -> serverConfig.globalScanCommands, val -> serverConfig.globalScanCommands = new ArrayList<>(val))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build())
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("option.fieldguide.category_scan_commands"))
                        .description(OptionDescription.of(Component.translatable("option.fieldguide.category_scan_commands.tooltip")))
                        .binding(new ArrayList<>(), () -> convertMapToList(serverConfig.categoryScanCommands), val -> serverConfig.categoryScanCommands = convertListToMap(val))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build())
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("option.fieldguide.entry_scan_commands"))
                        .description(OptionDescription.of(Component.translatable("option.fieldguide.entry_scan_commands.tooltip")))
                        .binding(new ArrayList<>(), () -> convertMapToList(serverConfig.entryScanCommands), val -> serverConfig.entryScanCommands = convertListToMap(val))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build());

        // Exposure
        ConfigCategory.Builder exposureCat = ConfigCategory.createBuilder()
                .name(Component.translatable("category.fieldguide.exposure"))
                .option(createBoolOption("exposure.add_photograph_button", true, () -> clientConfig.exposureAddPhotographButton, val -> clientConfig.exposureAddPhotographButton = val))
                .option(createBoolOption("exposure.unlock_via_photograph", true, () -> serverConfig.exposureUnlockViaPhotograph, val -> serverConfig.exposureUnlockViaPhotograph = val))
                .option(createBoolOption("exposure.show_photographs_in_grid", true, () -> clientConfig.exposureShowPhotographsInGrid, val -> clientConfig.exposureShowPhotographsInGrid = val));

        // Seasons
        ConfigCategory.Builder seasonsCat = ConfigCategory.createBuilder()
                .name(Component.translatable("category.fieldguide.seasons"))
                .option(createBoolOption("show_season_icons", true, () -> clientConfig.showSeasonIcons, val -> clientConfig.showSeasonIcons = val));

        return builder
                .category(general.build())
                .category(interfaceCat.build())
                .category(commandCat.build())
                .category(exposureCat.build())
                .category(seasonsCat.build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> createBoolOption(String name, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("option.fieldguide." + name))
                .description(OptionDescription.of(Component.translatable("option.fieldguide." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<String> createStringOption(String name, String defaultValue, Supplier<String> getter, Consumer<String> setter) {
        return Option.<String>createBuilder()
                .name(Component.translatable("option.fieldguide." + name))
                .description(OptionDescription.of(Component.translatable("option.fieldguide." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(StringControllerBuilder::create)
                .build();
    }

    private static Option<Integer> createIntOption(String name, int defaultValue, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("option.fieldguide." + name))
                .description(OptionDescription.of(Component.translatable("option.fieldguide." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(min, max))
                .build();
    }

    private static Option<Double> createDoubleOption(String name, double defaultValue, double min, double max, Supplier<Double> getter, Consumer<Double> setter) {
        return Option.<Double>createBuilder()
                .name(Component.translatable("option.fieldguide." + name))
                .description(OptionDescription.of(Component.translatable("option.fieldguide." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(opt -> DoubleFieldControllerBuilder.create(opt).range(min, max))
                .build();
    }

    private static List<String> convertMapToList(Map<String, List<String>> map) {
        List<String> list = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                list.add(entry.getKey() + "=" + String.join(";", entry.getValue()));
            }
        }
        return list;
    }

    private static Map<String, List<String>> convertListToMap(List<String> list) {
        Map<String, List<String>> map = new HashMap<>();
        if (list != null) {
            for (String s : list) {
                String[] parts = s.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String[] commands = parts[1].split(";");
                    List<String> commandList = new ArrayList<>();
                    for (String cmd : commands) {
                        if (!cmd.trim().isEmpty()) {
                            commandList.add(cmd.trim());
                        }
                    }
                    map.put(key, commandList);
                }
            }
        }
        return map;
    }
}

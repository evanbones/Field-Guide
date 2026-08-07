package com.evandev.fieldguide.config;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ClientConfig clientConfig = ClientConfig.get();
        ServerConfig serverConfig = ServerConfig.getLocal();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.fieldguide.config"));

        builder.setSavingRunnable(() -> {
            ClientConfig.save();
            ServerConfig.save();
            ClientFieldGuideManager.clearCache();

            if (Minecraft.getInstance().hasSingleplayerServer() && Minecraft.getInstance().getSingleplayerServer() != null) {
                Minecraft.getInstance().getSingleplayerServer().execute(() -> ServerFieldGuideManager.getInstance().reload(Minecraft.getInstance().getSingleplayerServer()));
            }
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General (Gameplay)
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.fieldguide.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_field_guide_item"), serverConfig.enableFieldGuideItem)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_field_guide_item.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableFieldGuideItem = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_lens_item"), serverConfig.enableLensItem)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_lens_item.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableLensItem = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.require_item_to_open"), serverConfig.requireItemToOpen)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.require_item_to_open.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.requireItemToOpen = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_copying_pages"), serverConfig.enableCopyingPages)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.enable_copying_pages.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableCopyingPages = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.default_screen"), clientConfig.defaultScreen)
                .setDefaultValue("last_opened_screen")
                .setTooltip(Component.translatable("option.fieldguide.default_screen.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.defaultScreen = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.hide_tabs_until_unlocked"), serverConfig.hideTabsUntilUnlocked)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.hide_tabs_until_unlocked.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.hideTabsUntilUnlocked = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_undiscovered_names"), serverConfig.showUndiscoveredNames)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.show_undiscovered_names.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.showUndiscoveredNames = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.hide_undiscovered_from_search"), serverConfig.hideUndiscoveredFromSearch)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.hide_undiscovered_from_search.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.hideUndiscoveredFromSearch = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_scanning"), serverConfig.disableScanning)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_scanning.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableScanning = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_spyglass_scanning"), serverConfig.enableSpyglassScanning)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.enable_spyglass_scanning.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableSpyglassScanning = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_naked_eye_scanning"), serverConfig.enableNakedEyeScanning)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_naked_eye_scanning.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableNakedEyeScanning = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.scan_speed"), serverConfig.scanSpeed)
                .setDefaultValue(1.0D)
                .setMin(0.1D)
                .setMax(10.0D)
                .setTooltip(Component.translatable("option.fieldguide.scan_speed.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.scanSpeed = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.min_scan_hitbox_size"), serverConfig.minScanHitboxSize)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(20.0D)
                .setTooltip(Component.translatable("option.fieldguide.min_scan_hitbox_size.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.minScanHitboxSize = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.spyglass_scan_distance"), serverConfig.spyglassScanDistance)
                .setDefaultValue(64.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.spyglass_scan_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.spyglassScanDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_spyglass_discovery"), serverConfig.enableSpyglassDiscovery)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_spyglass_discovery.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableSpyglassDiscovery = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.naked_eye_scan_distance"), serverConfig.nakedEyeScanDistance)
                .setDefaultValue(10.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.naked_eye_scan_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.nakedEyeScanDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_naked_eye_discovery"), serverConfig.enableNakedEyeDiscovery)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_naked_eye_discovery.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableNakedEyeDiscovery = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.naked_eye_discovery_distance"), serverConfig.nakedEyeDiscoveryDistance)
                .setDefaultValue(8.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.naked_eye_discovery_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.nakedEyeDiscoveryDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_field_guide_scanning"), serverConfig.enableFieldGuideScanning)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.enable_field_guide_scanning.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableFieldGuideScanning = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.field_guide_scan_distance"), serverConfig.fieldGuideScanDistance)
                .setDefaultValue(4.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.field_guide_scan_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.fieldGuideScanDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.lens_scan_distance"), serverConfig.lensScanDistance)
                .setDefaultValue(6.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.lens_scan_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.lensScanDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_lens_discovery"), serverConfig.enableLensDiscovery)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.enable_lens_discovery.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableLensDiscovery = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.lens_discovery_distance"), serverConfig.lensDiscoveryDistance)
                .setDefaultValue(24.0D)
                .setMin(1.0D)
                .setMax(256.0D)
                .setTooltip(Component.translatable("option.fieldguide.lens_discovery_distance.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.lensDiscoveryDistance = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.grant_xp_on_scan"), serverConfig.grantXpOnScan)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.grant_xp_on_scan.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.grantXpOnScan = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.enable_reliable_remover"), serverConfig.enableReliableRemover)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.enable_reliable_remover.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.enableReliableRemover = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.xp_amount_on_scan"), serverConfig.xpAmountOnScan)
                .setDefaultValue(5)
                .setMin(0)
                .setMax(1000)
                .setTooltip(Component.translatable("option.fieldguide.xp_amount_on_scan.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.xpAmountOnScan = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_loot_display"), serverConfig.disableLootDisplay)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_loot_display.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableLootDisplay = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_biome_display"), serverConfig.disableBiomeDisplay)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_biome_display.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableBiomeDisplay = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_variants"), serverConfig.disableVariants)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_variants.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableVariants = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_editing_descriptions"), serverConfig.disableEditingDescriptions)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_editing_descriptions.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableEditingDescriptions = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.disable_editing_names"), serverConfig.disableEditingNames)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.disable_editing_names.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.disableEditingNames = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_toasts"), clientConfig.showToasts)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_toasts.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showToasts = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.keep_silhouette"), serverConfig.keepSilhouetteWhenUnlocked)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.keep_silhouette.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.keepSilhouetteWhenUnlocked = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.unlock_variants"), serverConfig.unlockAllVariants)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.unlock_variants.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.unlockAllVariants = newValue)
                .build());

        // Interface (Visuals)
        ConfigCategory interfaceCat = builder.getOrCreateCategory(Component.translatable("category.fieldguide.interface"));

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_unlock_date"), clientConfig.showUnlockDate)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_unlock_date.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showUnlockDate = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.use_real_world_date"), clientConfig.useRealWorldDate)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.fieldguide.use_real_world_date.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.useRealWorldDate = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.play_scanning_sound"), clientConfig.playScanningSound)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.play_scanning_sound.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.playScanningSound = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.play_unlock_sound"), clientConfig.playUnlockSound)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.play_unlock_sound.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.playUnlockSound = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_scan_icon"), clientConfig.showScanIcon)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_scan_icon.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showScanIcon = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.scan_icon_y_offset"), clientConfig.scanIconYOffset)
                .setDefaultValue(2)
                .setTooltip(Component.translatable("option.fieldguide.scan_icon_y_offset.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.scanIconYOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.scan_icon_x_offset"), clientConfig.scanIconXOffset)
                .setDefaultValue(30)
                .setTooltip(Component.translatable("option.fieldguide.scan_icon_x_offset.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.scanIconXOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_out_of_range_overlay"), clientConfig.showOutOfRangeOverlay)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_out_of_range_overlay.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showOutOfRangeOverlay = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.scan_overlay_color"), clientConfig.scanOverlayColor)
                .setDefaultValue("#F9EED0")
                .setTooltip(Component.translatable("option.fieldguide.scan_overlay_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.scanOverlayColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.scan_overlay_alpha"), clientConfig.scanOverlayAlpha)
                .setDefaultValue(0.5D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("option.fieldguide.scan_overlay_alpha.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.scanOverlayAlpha = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_pause_button"), clientConfig.showPauseMenuButton)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_pause_button.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showPauseMenuButton = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.pause_button_x"), clientConfig.pauseButtonXOffset)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("option.fieldguide.pause_button_x.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.pauseButtonXOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.pause_button_y"), clientConfig.pauseButtonYOffset)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("option.fieldguide.pause_button_y.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.pauseButtonYOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_inventory_button"), clientConfig.showInventoryButton)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_inventory_button.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showInventoryButton = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.inventory_button_x"), clientConfig.inventoryButtonXOffset)
                .setDefaultValue(126)
                .setTooltip(Component.translatable("option.fieldguide.inventory_button_x.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.inventoryButtonXOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startIntField(Component.translatable("option.fieldguide.inventory_button_y"), clientConfig.inventoryButtonYOffset)
                .setDefaultValue(61)
                .setTooltip(Component.translatable("option.fieldguide.inventory_button_y.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.inventoryButtonYOffset = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.text_color"), clientConfig.textColor)
                .setDefaultValue("#8A5E3B")
                .setTooltip(Component.translatable("option.fieldguide.text_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.textColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.text_title_color"), clientConfig.textTitleColor)
                .setDefaultValue("#704623")
                .setTooltip(Component.translatable("option.fieldguide.text_title_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.textTitleColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.text_muted_color"), clientConfig.textMutedColor)
                .setDefaultValue("#C7A875")
                .setTooltip(Component.translatable("option.fieldguide.text_muted_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.textMutedColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.text_cursor_color"), clientConfig.textCursorColor)
                .setDefaultValue("#0xFF704623")
                .setTooltip(Component.translatable("option.fieldguide.text_cursor_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.textCursorColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.page_number_color"), clientConfig.pageNumberColor)
                .setDefaultValue("#C7A875")
                .setTooltip(Component.translatable("option.fieldguide.page_number_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.pageNumberColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.list_silhouette_color"), clientConfig.listSilhouetteColor)
                .setDefaultValue("#DDC69B")
                .setTooltip(Component.translatable("option.fieldguide.list_silhouette_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.listSilhouetteColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.list_silhouette_alpha"), clientConfig.listSilhouetteAlpha)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("option.fieldguide.list_silhouette_alpha.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.listSilhouetteAlpha = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.list_unlocked_silhouette_color"), clientConfig.listUnlockedSilhouetteColor)
                .setDefaultValue("#DDC69B")
                .setTooltip(Component.translatable("option.fieldguide.list_unlocked_silhouette_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.listUnlockedSilhouetteColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.list_unlocked_silhouette_alpha"), clientConfig.listUnlockedSilhouetteAlpha)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("option.fieldguide.list_unlocked_silhouette_alpha.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.listUnlockedSilhouetteAlpha = newValue)
                .build());


        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.details_silhouette_color"), clientConfig.detailsSilhouetteColor)
                .setDefaultValue("#DDC69B")
                .setTooltip(Component.translatable("option.fieldguide.details_silhouette_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.detailsSilhouetteColor = newValue)
                .build());


        interfaceCat.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.details_silhouette_alpha"), clientConfig.detailsSilhouetteAlpha)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("option.fieldguide.details_silhouette_alpha.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.detailsSilhouetteAlpha = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startStrField(Component.translatable("option.fieldguide.details_unlocked_silhouette_color"), clientConfig.detailsUnlockedSilhouetteColor)
                .setDefaultValue("#DDC69B")
                .setTooltip(Component.translatable("option.fieldguide.details_unlocked_silhouette_color.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.detailsUnlockedSilhouetteColor = newValue)
                .build());

        interfaceCat.addEntry(entryBuilder.startDoubleField(Component.translatable("option.fieldguide.details_unlocked_silhouette_alpha"), clientConfig.detailsUnlockedSilhouetteAlpha)
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(1.0D)
                .setTooltip(Component.translatable("option.fieldguide.details_unlocked_silhouette_alpha.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.detailsUnlockedSilhouetteAlpha = newValue)
                .build());

        // Commands
        ConfigCategory commandCat = builder.getOrCreateCategory(Component.translatable("category.fieldguide.commands"));

        commandCat.addEntry(entryBuilder.startStrList(Component.translatable("option.fieldguide.global_scan_commands"), serverConfig.globalScanCommands)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("option.fieldguide.global_scan_commands.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.globalScanCommands = newValue)
                .build());

        commandCat.addEntry(entryBuilder.startStrList(Component.translatable("option.fieldguide.category_scan_commands"), convertMapToList(serverConfig.categoryScanCommands))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("option.fieldguide.category_scan_commands.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.categoryScanCommands = convertListToMap(newValue))
                .build());

        commandCat.addEntry(entryBuilder.startStrList(Component.translatable("option.fieldguide.entry_scan_commands"), convertMapToList(serverConfig.entryScanCommands))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.translatable("option.fieldguide.entry_scan_commands.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.entryScanCommands = convertListToMap(newValue))
                .build());

        // Exposure
        ConfigCategory exposureCat = builder.getOrCreateCategory(Component.translatable("category.fieldguide.exposure"));

        exposureCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.exposure.add_photograph_button"), clientConfig.exposureAddPhotographButton)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.exposure.add_photograph_button.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.exposureAddPhotographButton = newValue)
                .build());

        exposureCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.exposure.unlock_via_photograph"), serverConfig.exposureUnlockViaPhotograph)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.exposure.unlock_via_photograph.tooltip"))
                .setSaveConsumer(newValue -> serverConfig.exposureUnlockViaPhotograph = newValue)
                .build());

        exposureCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.exposure.show_photographs_in_grid"), clientConfig.exposureShowPhotographsInGrid)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.exposure.show_photographs_in_grid.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.exposureShowPhotographsInGrid = newValue)
                .build());

        // Seasons
        ConfigCategory seasonsCat = builder.getOrCreateCategory(Component.translatable("category.fieldguide.seasons"));

        seasonsCat.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.fieldguide.show_season_icons"), clientConfig.showSeasonIcons)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.fieldguide.show_season_icons.tooltip"))
                .setSaveConsumer(newValue -> clientConfig.showSeasonIcons = newValue)
                .build());

        return builder.build();
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

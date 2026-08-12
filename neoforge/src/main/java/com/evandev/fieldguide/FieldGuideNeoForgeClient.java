package com.evandev.fieldguide;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.ModRenderTypes;
import com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen;
import com.evandev.fieldguide.config.ClothConfigIntegration;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.network.ProgressUpdatePacket;
import com.evandev.fieldguide.network.SyncCategoriesPacket;
import com.evandev.fieldguide.network.SyncConfigPacket;
import com.evandev.fieldguide.network.SyncLootPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.io.IOException;

public class FieldGuideNeoForgeClient {

    public static void handleSyncLoot(SyncLootPacket packet) {
        ClientFieldGuideManager.getInstance().updateLootCache(packet.lootCache(), packet.clearCache());
        if (Minecraft.getInstance().screen instanceof FieldGuideEntryScreen screen) {
            screen.refresh();
        }
    }

    public static void handleSyncConfig(SyncConfigPacket packet) {
        ServerConfig synced = ServerConfig.fromJson(packet.configJson());
        ServerConfig.setSyncedConfig(synced);
    }

    public static void handleSyncCategories(SyncCategoriesPacket packet) {
        ClientFieldGuideManager.getInstance().updateCategoriesFromServer(
                packet.getCategories(),
                packet.getEntries(),
                packet.getRedirects(),
                packet.shouldClearCache(),
                packet.shouldResolveEntries()
        );

        ClientFieldGuideManager.getInstance().updateModifiers(
                packet.getBiomeAdditions(),
                packet.getBiomeRemovals(),
                packet.getLootAdditions(),
                packet.getLootRemovals(),
                packet.shouldClearCache()
        );

        ClientFieldGuideManager.getInstance().updateVariants(packet.getVariants());
    }

    public static void handleProgressUpdate(ProgressUpdatePacket packet) {
        ClientFieldGuideManager.getInstance().applyServerUpdate(packet);
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            try {
                ModRenderTypes.registerShaders(instance -> {
                    String shaderName = instance.getName();
                    event.registerShader(instance, loadedShader -> {
                        if (shaderName.contains("fieldguide_scan_block")) {
                            ModRenderTypes.SCAN_BLOCK_SHADER = loadedShader;
                        } else if (shaderName.contains("fieldguide_scan_entity")) {
                            ModRenderTypes.SCAN_ENTITY_SHADER = loadedShader;
                        }
                    });
                }, event.getResourceProvider());
            } catch (IOException e) {
                throw new RuntimeException("Failed to register Field Guide shaders", e);
            }
        }

        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(ClientFieldGuideManager.getInstance());
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            FieldGuideClient.init();
            event.register(FieldGuideClient.OPEN_GUIDE_KEY);
            event.register(FieldGuideClient.SCAN_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (ModList.get().isLoaded("cloth_config")) {
                ModLoadingContext.get().registerExtensionPoint(
                        IConfigScreenFactory.class,
                        () -> (client, parent) -> ClothConfigIntegration.createScreen(parent)
                );
            }
        }
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientNeoForgeEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            ClientFieldGuideManager.getInstance().onClientTick(client);
            FieldGuideClient.onClientTick();
        }

        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                FieldGuideClient.renderScanningIcon(event.getGuiGraphics(), partialTick);
            }
        }

        @SubscribeEvent
        public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            ClientFieldGuideManager.getInstance().onWorldLoad();
        }

        @SubscribeEvent
        public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientFieldGuideManager.getInstance().onWorldUnload();
        }
    }
}
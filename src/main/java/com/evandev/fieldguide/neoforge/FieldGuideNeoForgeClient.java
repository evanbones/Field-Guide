package com.evandev.fieldguide.neoforge;

//? if neoforge {
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.config.YaclConfigIntegration;
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

//? if <26.1 {
import com.evandev.fieldguide.client.ModRenderTypes;

import java.io.IOException;
//?}

//? if >=26.1 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public class FieldGuideNeoForgeClient {

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        //? if <26.1 {
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
        //?} else {
        /*public static void registerReloadListeners(AddClientReloadListenersEvent event) {
            event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client_data"), ClientFieldGuideManager.getInstance());
        *///?}
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            FieldGuideClient.init();
            event.register(FieldGuideClient.OPEN_GUIDE_KEY);
            event.register(FieldGuideClient.SCAN_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
                ModLoadingContext.get().registerExtensionPoint(
                        IConfigScreenFactory.class,
                        () -> (client, parent) -> YaclConfigIntegration.createScreen(parent)
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
                //? if <26.1 {
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                //?} else {
                /*float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                *///?}
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
//?}

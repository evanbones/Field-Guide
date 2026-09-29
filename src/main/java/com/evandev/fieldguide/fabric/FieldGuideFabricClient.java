package com.evandev.fieldguide.fabric;

//? if fabric {
/*import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if <26.1 {
import com.evandev.fieldguide.api.seasons.SeasonsAPI;
import com.evandev.fieldguide.client.ModRenderTypes;
import com.evandev.fieldguide.fabric.compat.fabricseasons.FabricSeasonsProvider;
import com.evandev.fieldguide.platform.Services;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

import java.io.IOException;
//?}

//? if >=26.1 {
/^import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import org.jspecify.annotations.NonNull;
^///?}

public class FieldGuideFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FieldGuideClient.init();
        //? if <26.1 {
        KeyBindingHelper.registerKeyBinding(FieldGuideClient.OPEN_GUIDE_KEY);
        KeyBindingHelper.registerKeyBinding(FieldGuideClient.SCAN_KEY);

        if (Services.PLATFORM.isModLoaded("seasons")) {
            SeasonsAPI.registerProvider(new FabricSeasonsProvider());
        }
        //?} else {
        /^KeyMappingHelper.registerKeyMapping(FieldGuideClient.OPEN_GUIDE_KEY);
        KeyMappingHelper.registerKeyMapping(FieldGuideClient.SCAN_KEY);
        ^///?}

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            //? if <26.1 {
            public ResourceLocation getFabricId() {
            //?} else {
            /^public @NonNull ResourceLocation getFabricId() {
            ^///?}
                return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mob_data");
            }

            @Override
            public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
                ClientFieldGuideManager.getInstance().onResourceManagerReload(resourceManager);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientFieldGuideManager.getInstance().onClientTick(client);
            FieldGuideClient.onClientTick();
        });

        receive(SyncLootPacket.TYPE, SyncLootPacket::handleClient);
        receive(SyncCategoriesPacket.TYPE, SyncCategoriesPacket::handleClient);
        receive(ProgressUpdatePacket.TYPE, ProgressUpdatePacket::handleClient);
        receive(SyncConfigPacket.TYPE, SyncConfigPacket::handleClient);
        receive(ExportContentPacket.TYPE, ExportContentPacket::handleClient);

        //? if <26.1 {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(
                        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "fieldguide_scan_block"),
                        DefaultVertexFormat.BLOCK,
                        program -> ModRenderTypes.SCAN_BLOCK_SHADER = program
                );

                context.register(
                        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "fieldguide_scan_entity"),
                        DefaultVertexFormat.NEW_ENTITY,
                        program -> ModRenderTypes.SCAN_ENTITY_SHADER = program
                );

            } catch (IOException e) {
                throw new RuntimeException("Failed to register fieldguide shaders", e);
            }
        });
        //?}

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientFieldGuideManager.getInstance().onWorldLoad();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientFieldGuideManager.getInstance().onWorldUnload()
        );
    }

    private static <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        //? if >=1.21 {
        ClientPlayNetworking.registerGlobalReceiver(type, (packet, context) -> context.client().execute(() -> handler.accept(packet)));
        //?} else {
        /^ClientPlayNetworking.registerGlobalReceiver(type.id(), (client, listener, buf, sender) -> {
            T packet = Services.NETWORK.decode(type, buf);
            client.execute(() -> handler.accept(packet));
        });
        ^///?}
    }
}
*///?}

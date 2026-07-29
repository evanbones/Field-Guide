package com.evandev.fieldguide;

import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class FieldGuideFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FieldGuideClient.init();
        KeyMappingHelper.registerKeyMapping(FieldGuideClient.OPEN_GUIDE_KEY);
        KeyMappingHelper.registerKeyMapping(FieldGuideClient.SCAN_KEY);

/*        if (Services.PLATFORM.isModLoaded("seasons")) {
            SeasonsAPI.registerProvider(new FabricSeasonsProvider());
        }*/

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public @NonNull Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "mob_data");
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

        ClientPlayNetworking.registerGlobalReceiver(SyncLootPacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                ClientFieldGuideManager.getInstance().updateLootCache(packet.lootCache(), packet.clearCache());

                if (context.client().gui.screen() instanceof FieldGuideEntryScreen screen) {
                    screen.refresh();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncCategoriesPacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();

                manager.updateCategoriesFromServer(
                        packet.getCategories(),
                        packet.getEntries(),
                        packet.getRedirects(),
                        packet.shouldClearCache(),
                        packet.shouldResolveEntries()
                );

                manager.updateModifiers(
                        packet.getBiomeAdditions(),
                        packet.getBiomeRemovals(),
                        packet.getLootAdditions(),
                        packet.getLootRemovals(),
                        packet.shouldClearCache()
                );

                manager.updateVariants(packet.getVariants());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ProgressUpdatePacket.TYPE, (packet, context) -> {
            context.client().execute(() -> ClientFieldGuideManager.getInstance().applyServerUpdate(packet));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncConfigPacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                ServerConfig synced = ServerConfig.fromJson(packet.configJson());
                ServerConfig.setSyncedConfig(synced);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ExportContentPacket.TYPE, (payload, context) -> {
            context.client().execute(payload::handleClient);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientFieldGuideManager.getInstance().onWorldLoad();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientFieldGuideManager.getInstance().onWorldUnload()
        );
    }
}
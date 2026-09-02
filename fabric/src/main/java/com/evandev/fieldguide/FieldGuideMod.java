package com.evandev.fieldguide;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.compat.exposure.ExposureFabricEventHandler;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.platform.FabricNetworkHelper;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FieldGuideMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> FieldGuideCommand.register(dispatcher));

        if (Services.PLATFORM.isModLoaded("exposure")) {
            ExposureFabricEventHandler.register();
        }

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                ServerFieldGuideManager.getInstance().reload(server);
            }
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(Constants.MOD_ID, "server_data");
            }

            @Override
            public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier barrier, @NotNull ResourceManager manager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor prepareExecutor, @NotNull Executor applyExecutor) {
                return ServerFieldGuideManager.getInstance().reload(barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerFieldGuideManager.getInstance().syncToPlayer(handler.player);
            FieldGuideProgressManager.getInstance().onPlayerJoin(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FieldGuideProgressManager.getInstance().onPlayerDisconnect(handler.player);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerFieldGuideManager.getInstance().onServerStarted(server);
            FieldGuideProgressManager.init(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FieldGuideProgressManager.shutdown();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FieldGuideProgressManager.getInstance().tick();
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.SCAN_UNLOCK_CHANNEL, (server, player, handler, buf, responseSender) -> {
            ScanUnlockPacket packet = new ScanUnlockPacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.MARK_SEEN_CHANNEL, (server, player, handler, buf, responseSender) -> {
            MarkSeenPacket packet = new MarkSeenPacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.UPDATE_ENTRY_DATA_CHANNEL, (server, player, handler, buf, responseSender) -> {
            UpdateEntryDataPacket packet = new UpdateEntryDataPacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.UPDATE_JOURNAL_CHANNEL, (server, player, handler, buf, responseSender) -> {
            UpdateJournalPacket packet = new UpdateJournalPacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.RIP_OUT_CHANNEL, (server, player, handler, buf, responseSender) -> {
            CopyPagePacket packet = new CopyPagePacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(FabricNetworkHelper.REQUEST_LOOT_CHANNEL, (server, player, handler, buf, responseSender) -> {
            RequestLootPacket packet = new RequestLootPacket(buf);
            server.execute(() -> packet.handleServer(player));
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayer player) {
                FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
                if (manager.wasRecentlyScanned(player, killedEntity.getId())) {
                    FieldGuideTriggers.SCAN_AND_KILL.trigger(player, killedEntity);
                }

                ResourceLocation entityId = EntryResolver.getEntryId(killedEntity.getType());
                PlayerFieldGuideProgress progress = manager.getProgress(player);
                if (progress != null) {
                    String variantId = null;
                    if (killedEntity instanceof Mob mob) {
                        String tracked = FieldGuideVariantManager.getTrackedVariantId(mob);
                        if (!tracked.isEmpty()) variantId = tracked;
                    }
                    progress.tryUnlock(player, entityId, variantId, EntryUnlockData.UnlockTrigger.KILL);
                }
            }
        });
    }
}

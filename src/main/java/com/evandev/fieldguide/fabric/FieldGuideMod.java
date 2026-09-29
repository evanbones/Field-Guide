package com.evandev.fieldguide.fabric;

//? if fabric {
/*import com.evandev.fieldguide.CommonClass;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

//? if <26.1 {
import com.evandev.fieldguide.fabric.compat.exposure.ExposureFabricEventHandler;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
//?}

//? if >=26.1 {
/^import net.minecraft.server.packs.resources.PreparableReloadListener;
^///?}

//? if >=1.21 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}

public class FieldGuideMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        clientbound(SyncCategoriesPacket.TYPE, SyncCategoriesPacket.CODEC);
        clientbound(SyncLootPacket.TYPE, SyncLootPacket.CODEC);
        clientbound(ExportContentPacket.TYPE, ExportContentPacket.CODEC);
        clientbound(ProgressUpdatePacket.TYPE, ProgressUpdatePacket.CODEC);
        clientbound(SyncConfigPacket.TYPE, SyncConfigPacket.CODEC);

        serverbound(ScanUnlockPacket.TYPE, ScanUnlockPacket.CODEC);
        serverbound(MarkSeenPacket.TYPE, MarkSeenPacket.CODEC);
        serverbound(UpdateEntryDataPacket.TYPE, UpdateEntryDataPacket.CODEC);
        serverbound(UpdateJournalPacket.TYPE, UpdateJournalPacket.CODEC);
        serverbound(CopyPagePacket.TYPE, CopyPagePacket.CODEC);
        serverbound(RequestLootPacket.TYPE, RequestLootPacket.CODEC);

        receive(ScanUnlockPacket.TYPE, ScanUnlockPacket::handleServer);
        receive(MarkSeenPacket.TYPE, MarkSeenPacket::handleServer);
        receive(UpdateEntryDataPacket.TYPE, UpdateEntryDataPacket::handleServer);
        receive(UpdateJournalPacket.TYPE, UpdateJournalPacket::handleServer);
        receive(CopyPagePacket.TYPE, CopyPagePacket::handleServer);
        receive(RequestLootPacket.TYPE, RequestLootPacket::handleServer);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> FieldGuideCommand.register(dispatcher));

        //? if <26.1 {
        if (Services.PLATFORM.isModLoaded("exposure")) {
            ExposureFabricEventHandler.register();
        }
        //?}

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                ServerFieldGuideManager.getInstance().reload(server);
            }
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "server_data");
            }

            @Override
            //? if <26.1 {
            public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier barrier, @NotNull ResourceManager manager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor prepareExecutor, @NotNull Executor applyExecutor) {
                return ServerFieldGuideManager.getInstance().reload(barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
            //?} else {
            /^public @NotNull CompletableFuture<Void> reload(@NotNull PreparableReloadListener.SharedState state, @NotNull Executor prepareExecutor, @NotNull PreparableReloadListener.PreparationBarrier barrier, @NotNull Executor applyExecutor) {
                return ServerFieldGuideManager.getInstance().reload(state, prepareExecutor, barrier, applyExecutor);
            ^///?}
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CommonClass.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FieldGuideProgressManager.getInstance().onPlayerDisconnect(handler.player));
        ServerLifecycleEvents.SERVER_STARTED.register(CommonClass::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> FieldGuideProgressManager.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(server -> FieldGuideProgressManager.getInstance().tick());
        ServerLivingEntityEvents.AFTER_DEATH.register(CommonClass::onEntityKilled);
    }

    private static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        //? if >=1.21 && <26.1 {
        PayloadTypeRegistry.playS2C().register(type, codec);
        //?} else if >=26.1 {
        /^PayloadTypeRegistry.clientboundPlay().register(type, codec);
        ^///?} else {
        /^Services.NETWORK.registerCodec(type, codec);
        ^///?}
    }

    private static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        //? if >=1.21 && <26.1 {
        PayloadTypeRegistry.playC2S().register(type, codec);
        //?} else if >=26.1 {
        /^PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ^///?} else {
        /^Services.NETWORK.registerCodec(type, codec);
        ^///?}
    }

    private static <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, BiConsumer<T, ServerPlayer> handler) {
        //? if >=1.21 {
        ServerPlayNetworking.registerGlobalReceiver(type, (packet, context) -> context.server().execute(() -> handler.accept(packet, context.player())));
        //?} else {
        /^ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, listener, buf, sender) -> {
            T packet = Services.NETWORK.decode(type, buf);
            server.execute(() -> handler.accept(packet, player));
        });
        ^///?}
    }
}
*///?}

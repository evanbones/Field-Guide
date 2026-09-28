package com.evandev.fieldguide.neoforge;

//? if neoforge {
import com.evandev.fieldguide.CommonClass;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.platform.RegistryHelper;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

//? if <26.1 {
import com.evandev.fieldguide.neoforge.compat.exposure.ExposureNeoForgeEventHandler;
import com.evandev.fieldguide.neoforge.compat.mixedlitter.MixedLitterCompat;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
//?}

//? if >=26.1 {
/*import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
*///?}

@Mod(Constants.MOD_ID)
public class FieldGuideMod {

    //? if <26.2 {
    public FieldGuideMod(IEventBus modEventBus) {
    //?} else {
    /*public FieldGuideMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
    *///?}
        RegistryHelper.init(modEventBus);
        CommonClass.init();

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerPayloads);

    //? if <26.1 {
        if (ModList.get().isLoaded("exposure")) {
            registerExposureCompat();
        }

        if (ModList.get().isLoaded("mixed_litter")) {
            FieldGuideVariantManager.registerProvider(Mob.class, new MixedLitterCompat.MixedLitterVariantProvider());
        }

        if (ModList.get().isLoaded("primal")) {
            FieldGuideVariantManager.registerProvider(Mob.class, new com.evandev.fieldguide.neoforge.compat.primal.PrimalVariantProvider());
        }
    }

    private void registerExposureCompat() {
        NeoForge.EVENT_BUS.register(ExposureNeoForgeEventHandler.class);
    //?}
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional();

        registrar.playToClient(SyncLootPacket.TYPE, SyncLootPacket.CODEC, clientHandler(SyncLootPacket::handleClient));
        registrar.playToClient(SyncCategoriesPacket.TYPE, SyncCategoriesPacket.CODEC, clientHandler(SyncCategoriesPacket::handleClient));
        registrar.playToClient(ProgressUpdatePacket.TYPE, ProgressUpdatePacket.CODEC, clientHandler(ProgressUpdatePacket::handleClient));
        registrar.playToClient(ExportContentPacket.TYPE, ExportContentPacket.CODEC, clientHandler(ExportContentPacket::handleClient));
        registrar.playToClient(SyncConfigPacket.TYPE, SyncConfigPacket.CODEC, clientHandler(SyncConfigPacket::handleClient));

        registrar.playToServer(ScanUnlockPacket.TYPE, ScanUnlockPacket.CODEC, serverHandler(ScanUnlockPacket::handleServer));
        registrar.playToServer(MarkSeenPacket.TYPE, MarkSeenPacket.CODEC, serverHandler(MarkSeenPacket::handleServer));
        registrar.playToServer(UpdateEntryDataPacket.TYPE, UpdateEntryDataPacket.CODEC, serverHandler(UpdateEntryDataPacket::handleServer));
        registrar.playToServer(UpdateJournalPacket.TYPE, UpdateJournalPacket.CODEC, serverHandler(UpdateJournalPacket::handleServer));
        registrar.playToServer(CopyPagePacket.TYPE, CopyPagePacket.CODEC, serverHandler(CopyPagePacket::handleServer));
        registrar.playToServer(RequestLootPacket.TYPE, RequestLootPacket.CODEC, serverHandler(RequestLootPacket::handleServer));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> clientHandler(Consumer<T> handler) {
        return (packet, context) -> context.enqueueWork(() -> handler.accept(packet));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> serverHandler(BiConsumer<T, ServerPlayer> handler) {
        return (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handler.accept(packet, player);
        });
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerFieldGuideManager.getInstance().reload(event.getPlayerList().getServer());
        }
    }

    @SubscribeEvent
    //? if <26.1 {
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ServerFieldGuideManager.getInstance());
    //?} else {
    /*public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "server_data"), ServerFieldGuideManager.getInstance());
    *///?}
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FieldGuideCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CommonClass.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        FieldGuideProgressManager.shutdown();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        FieldGuideProgressManager.getInstance().tick();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CommonClass.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FieldGuideProgressManager.getInstance().onPlayerDisconnect(player);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        CommonClass.onEntityKilled(event.getEntity(), event.getSource());
    }
}
//?}

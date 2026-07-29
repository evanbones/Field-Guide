package com.evandev.fieldguide;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.platform.NeoForgeRegistryHelper;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class FieldGuideMod {

    public FieldGuideMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        NeoForgeRegistryHelper.init(modEventBus);
        CommonClass.init();

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerPayloads);

        if (ModList.get().isLoaded("exposure")) {
            //registerExposureCompat();
        }

        if (ModList.get().isLoaded("mixed_litter")) {
            //FieldGuideVariantManager.registerProvider(Mob.class, new MixedLitterCompat.MixedLitterVariantProvider());
        }

        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            FieldGuideNeoForgeClient.init(modEventBus, modContainer);
        }
    }

/*
    private void registerExposureCompat() {
        NeoForge.EVENT_BUS.register(ExposureNeoForgeEventHandler.class);
    }
*/

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).versioned("1.0");

        // S2C
        registrar.playToClient(SyncLootPacket.TYPE, SyncLootPacket.CODEC, (packet, context) -> context.enqueueWork(() -> FieldGuideNeoForgeClient.handleSyncLoot(packet)));
        registrar.playToClient(SyncCategoriesPacket.TYPE, SyncCategoriesPacket.CODEC, (packet, context) -> context.enqueueWork(() -> FieldGuideNeoForgeClient.handleSyncCategories(packet)));
        registrar.playToClient(ProgressUpdatePacket.TYPE, ProgressUpdatePacket.CODEC, (packet, context) -> context.enqueueWork(() -> FieldGuideNeoForgeClient.handleProgressUpdate(packet)));
        registrar.playToClient(ExportContentPacket.TYPE, ExportContentPacket.CODEC, (packet, context) -> context.enqueueWork(packet::handleClient));
        registrar.playToClient(SyncConfigPacket.TYPE, SyncConfigPacket.CODEC, (packet, context) -> context.enqueueWork(() -> FieldGuideNeoForgeClient.handleSyncConfig(packet)));

        // C2S
        registrar.playToServer(ScanUnlockPacket.TYPE, ScanUnlockPacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
        registrar.playToServer(MarkSeenPacket.TYPE, MarkSeenPacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
        registrar.playToServer(UpdateEntryDataPacket.TYPE, UpdateEntryDataPacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
        registrar.playToServer(UpdateJournalPacket.TYPE, UpdateJournalPacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
        registrar.playToServer(CopyPagePacket.TYPE, CopyPagePacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
        registrar.playToServer(RequestLootPacket.TYPE, RequestLootPacket.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) packet.handleServer(sp);
        }));
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerFieldGuideManager.getInstance().reload(event.getPlayerList().getServer());
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "server_data"), ServerFieldGuideManager.getInstance());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FieldGuideCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerFieldGuideManager.getInstance().onServerStarted(event.getServer());
        FieldGuideProgressManager.init(event.getServer());
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
            ServerFieldGuideManager.getInstance().syncToPlayer(player);
            FieldGuideProgressManager.getInstance().onPlayerJoin(player);
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
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();

            if (manager.wasRecentlyScanned(player, event.getEntity().getId())) {
                FieldGuideTriggers.SCAN_AND_KILL.get().trigger(player, event.getEntity());
            }

            Identifier entityId = EntryResolver.getEntryId(event.getEntity().getType());
            PlayerFieldGuideProgress progress = manager.getProgress(player);

            if (progress != null) {
                String variantId = null;
                if (event.getEntity() instanceof Mob mob) {
                    VariantProvider<Mob> provider = FieldGuideVariantManager.getProvider(mob);
                    if (provider != null) {
                        VariantDef current = provider.getCurrent(mob);
                        if (current != null) variantId = current.id();
                    }
                }
                progress.tryUnlock(player, entityId, variantId, EntryUnlockData.UnlockTrigger.KILL);
            }
        }
    }
}
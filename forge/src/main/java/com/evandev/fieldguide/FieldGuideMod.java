package com.evandev.fieldguide;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.compat.exposure.ExposureForgeEventHandler;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.platform.ForgeNetworkHelper;
import com.evandev.fieldguide.platform.ForgeRegistryHelper;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class FieldGuideMod {

    public FieldGuideMod() {
        CommonClass.init();
        MinecraftForge.EVENT_BUS.register(this);

        if (Services.PLATFORM.isModLoaded("exposure")) {
            MinecraftForge.EVENT_BUS.register(ExposureForgeEventHandler.class);
        }

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ForgeRegistryHelper.init(modEventBus);
    }

    public static void handleProgressUpdate(ProgressUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FieldGuideForgeClient.handleProgressUpdate(packet)));
        context.setPacketHandled(true);
    }

    public static void handleSyncLoot(SyncLootPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FieldGuideForgeClient.handleSyncLoot(packet)));
        context.setPacketHandled(true);
    }

    public static void handleSyncConfig(SyncConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FieldGuideForgeClient.handleSyncConfig(packet)));
        context.setPacketHandled(true);
    }

    public static void handleSyncCategories(SyncCategoriesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FieldGuideForgeClient.handleSyncCategories(packet)));
        context.setPacketHandled(true);
    }

    public static void handleExportContent(ExportContentPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> packet::handleClient));
        context.setPacketHandled(true);
    }

    public static void handleScanUnlock(ScanUnlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleMarkSeen(MarkSeenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleUpdateEntryData(UpdateEntryDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleUpdateJournal(UpdateJournalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleCopy(CopyPagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void handleRequestLoot(RequestLootPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                packet.handleServer(player);
            }
        });
        context.setPacketHandled(true);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ForgeNetworkHelper.register();
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ServerFieldGuideManager.getInstance());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerFieldGuideManager.getInstance().reload(event.getPlayerList().getServer());
        }
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FieldGuideProgressManager.getInstance().tick();
        }
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
                FieldGuideTriggers.SCAN_AND_KILL.trigger(player, event.getEntity());
            }

            ResourceLocation entityId = EntryResolver.getEntryId(event.getEntity().getType());
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                String variantId = null;
                if (event.getEntity() instanceof Mob mob) {
                    String tracked = FieldGuideVariantManager.getTrackedVariantId(mob);
                    if (!tracked.isEmpty()) variantId = tracked;
                }
                progress.tryUnlock(player, entityId, variantId, EntryUnlockData.UnlockTrigger.KILL);
            }
        }
    }
}

package com.evandev.fieldguide.forge;

//? if forge {
/*import com.evandev.fieldguide.CommonClass;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.forge.compat.exposure.ExposureForgeEventHandler;
import com.evandev.fieldguide.network.*;
import com.evandev.fieldguide.platform.RegistryHelper;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.command.FieldGuideCommand;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class FieldGuideMod {

    public FieldGuideMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        RegistryHelper.init(modEventBus);
        CommonClass.init();

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);

        if (Services.PLATFORM.isModLoaded("exposure")) {
            MinecraftForge.EVENT_BUS.register(ExposureForgeEventHandler.class);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FieldGuideMod::registerPackets);
    }

    private static void registerPackets() {
        clientbound(SyncLootPacket.class, SyncLootPacket.TYPE, SyncLootPacket.CODEC, SyncLootPacket::handleClient);
        clientbound(SyncCategoriesPacket.class, SyncCategoriesPacket.TYPE, SyncCategoriesPacket.CODEC, SyncCategoriesPacket::handleClient);
        clientbound(ProgressUpdatePacket.class, ProgressUpdatePacket.TYPE, ProgressUpdatePacket.CODEC, ProgressUpdatePacket::handleClient);
        clientbound(ExportContentPacket.class, ExportContentPacket.TYPE, ExportContentPacket.CODEC, ExportContentPacket::handleClient);
        clientbound(SyncConfigPacket.class, SyncConfigPacket.TYPE, SyncConfigPacket.CODEC, SyncConfigPacket::handleClient);

        serverbound(ScanUnlockPacket.class, ScanUnlockPacket.TYPE, ScanUnlockPacket.CODEC, ScanUnlockPacket::handleServer);
        serverbound(MarkSeenPacket.class, MarkSeenPacket.TYPE, MarkSeenPacket.CODEC, MarkSeenPacket::handleServer);
        serverbound(UpdateEntryDataPacket.class, UpdateEntryDataPacket.TYPE, UpdateEntryDataPacket.CODEC, UpdateEntryDataPacket::handleServer);
        serverbound(UpdateJournalPacket.class, UpdateJournalPacket.TYPE, UpdateJournalPacket.CODEC, UpdateJournalPacket::handleServer);
        serverbound(CopyPagePacket.class, CopyPagePacket.TYPE, CopyPagePacket.CODEC, CopyPagePacket::handleServer);
        serverbound(RequestLootPacket.class, RequestLootPacket.TYPE, RequestLootPacket.CODEC, RequestLootPacket::handleServer);
    }

    private static <T extends CustomPacketPayload> void clientbound(Class<T> type, CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        Services.NETWORK.registerForge(type, id, codec, NetworkDirection.PLAY_TO_CLIENT, (packet, context) -> handler.accept(packet));
    }

    private static <T extends CustomPacketPayload> void serverbound(Class<T> type, CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        Services.NETWORK.registerForge(type, id, codec, NetworkDirection.PLAY_TO_SERVER, (packet, context) -> {
            ServerPlayer player = context.getSender();
            if (player != null) handler.accept(packet, player);
        });
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            ServerFieldGuideManager.getInstance().reload(event.getPlayerList().getServer());
        }
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ServerFieldGuideManager.getInstance());
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
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FieldGuideProgressManager.getInstance().tick();
        }
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
*///?}

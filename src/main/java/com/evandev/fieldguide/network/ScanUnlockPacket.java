package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.server.scan.ScanVerifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class ScanUnlockPacket implements CustomPacketPayload {

    public static final Type<ScanUnlockPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "scan_unlock"));

    public static final StreamCodec<FriendlyByteBuf, ScanUnlockPacket> CODEC = StreamCodec.ofMember(
            ScanUnlockPacket::encode,
            ScanUnlockPacket::new
    );

    private final ResourceLocation entryId;
    private final String variantId;
    private final ResourceLocation scannedTargetId;
    private final BlockPos targetBlockPos;
    private final int targetEntityId;

    public ScanUnlockPacket(
            ResourceLocation entryId,
            String variantId,
            ResourceLocation scannedTargetId,
            BlockPos targetBlockPos,
            int targetEntityId
    ) {
        this.entryId = entryId;
        this.variantId = variantId != null ? variantId : "";
        this.scannedTargetId = scannedTargetId;
        this.targetBlockPos = targetBlockPos;
        this.targetEntityId = targetEntityId;
    }

    public ScanUnlockPacket(FriendlyByteBuf buf) {
        this.entryId = buf.readResourceLocation();
        this.variantId = buf.readUtf();
        this.scannedTargetId = buf.readResourceLocation();
        this.targetBlockPos = buf.readNullable(b -> b.readBlockPos());
        this.targetEntityId = buf.readVarInt();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(entryId);
        buf.writeUtf(variantId);
        buf.writeResourceLocation(scannedTargetId);
        buf.writeNullable(targetBlockPos, (b, pos) -> b.writeBlockPos(pos));
        buf.writeVarInt(targetEntityId);
    }

    public void handleServer(ServerPlayer player) {
        if (player == null) return;

        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        PlayerFieldGuideProgress progress = manager.getProgress(player);
        if (progress == null) return;

        if (!manager.isValidEntry(entryId)) {
            Constants.LOG.warn("Scan failed: entryId {} is not valid on the server.", entryId);
            return;
        }

        if (!ScanVerifier.verifyScan(player, entryId, scannedTargetId, targetBlockPos, targetEntityId)) {
            Constants.LOG.warn("Scan failed: ScanVerifier rejected the scan for entryId {}.", entryId);
            return;
        }

        if (targetEntityId != 0) {
            //? if <26.1 {
            Entity entity = player.serverLevel().getEntity(targetEntityId);
            //?} else {
            /*Entity entity = player.level().getEntity(targetEntityId);
            *///?}
            if (entity != null) {
                FieldGuideTriggers.SCAN_ENTITY.get().trigger(player, entity);
                manager.recordScan(player, targetEntityId);
            }
        }

        progress.tryUnlock(player, entryId, variantId, EntryUnlockData.UnlockTrigger.SCAN);
    }
}

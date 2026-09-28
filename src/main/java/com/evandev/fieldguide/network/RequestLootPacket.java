package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record RequestLootPacket(ResourceLocation entryId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestLootPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "request_loot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLootPacket> CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    RequestLootPacket::entryId,
                    RequestLootPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleServer(ServerPlayer player) {
        ServerFieldGuideManager.getInstance().syncLootToPlayer(player, this.entryId);
    }
}

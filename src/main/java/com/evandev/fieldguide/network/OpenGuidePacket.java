package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.FieldGuideClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class OpenGuidePacket implements CustomPacketPayload {
    public static final Type<OpenGuidePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "open_guide"));
    public static final StreamCodec<FriendlyByteBuf, OpenGuidePacket> CODEC = StreamCodec.ofMember(OpenGuidePacket::encode, OpenGuidePacket::new);

    public OpenGuidePacket() {
    }

    public OpenGuidePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        FieldGuideClient.showGuide();
    }
}

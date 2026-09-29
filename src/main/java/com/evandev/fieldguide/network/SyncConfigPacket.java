package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncConfigPacket(String configJson) implements CustomPacketPayload {

    public static final Type<SyncConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPacket> CODEC = StreamCodec.ofMember(SyncConfigPacket::encode, SyncConfigPacket::new);

    public SyncConfigPacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(configJson);
    }

    public SyncConfigPacket(ServerConfig config) {
        this(config.toJson());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        // The integrated server shares this JVM's config statics; a synced snapshot would shadow the live config for the server too
        if (Minecraft.getInstance().isLocalServer()) return;
        ServerConfig.setSyncedConfig(ServerConfig.fromJson(configJson));
    }
}

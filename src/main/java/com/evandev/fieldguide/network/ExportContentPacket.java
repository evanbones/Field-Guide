package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.util.IconCacheManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ExportContentPacket implements CustomPacketPayload {
    public static final Type<ExportContentPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "export_content"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportContentPacket> CODEC = StreamCodec.ofMember(ExportContentPacket::encode, ExportContentPacket::new);

    private final String type;

    public ExportContentPacket(String type) {
        this.type = type;
    }

    public ExportContentPacket(RegistryFriendlyByteBuf buf) {
        this.type = buf.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(type);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        if ("reload_cache".equals(type)) {
            ClientFieldGuideManager.clearCache();
            IconCacheManager.clearCache();
        } else {
            ClientFieldGuideManager.getInstance().exportToLang(type);
        }
    }
}

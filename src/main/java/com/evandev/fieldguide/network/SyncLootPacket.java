package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.evandev.fieldguide.util.ByteBufCollections;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncLootPacket(Map<ResourceLocation, List<ItemStack>> lootCache,
                             boolean clearCache) implements CustomPacketPayload {

    public static final Type<SyncLootPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_loot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLootPacket> CODEC = StreamCodec.ofMember(SyncLootPacket::encode, SyncLootPacket::new);

    public SyncLootPacket(RegistryFriendlyByteBuf buf) {
        this(ByteBufCollections.readMap(buf, HashMap::new, FriendlyByteBuf::readResourceLocation,
                b -> ByteBufCollections.readList(b, ByteBufCollections::readItem)), buf.readBoolean());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        ByteBufCollections.writeMap(buf, lootCache, FriendlyByteBuf::writeResourceLocation,
                (b, stacks) -> ByteBufCollections.writeCollection(b, stacks, ByteBufCollections::writeItem));
        buf.writeBoolean(clearCache);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        ClientFieldGuideManager.getInstance().updateLootCache(lootCache, clearCache);
        if (Minecraft.getInstance().screen instanceof FieldGuideEntryScreen screen) {
            screen.refresh();
        }
    }
}

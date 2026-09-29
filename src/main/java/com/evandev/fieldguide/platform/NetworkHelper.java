package com.evandev.fieldguide.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
*///?} else if neoforge {
import net.neoforged.neoforge.network.PacketDistributor;
//?}

//? if neoforge && >=26.1 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

//? if <1.21 {
/*import com.evandev.fieldguide.Constants;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
*///?}

//? if forge {
/*import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
*///?}

public class NetworkHelper {
    //? if forge {
    /*private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );
    private int nextMessageId;
    *///?}

    //? if <1.21 {
    /*private final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ?>> codecs = new HashMap<>();

    public <T extends CustomPacketPayload> void registerCodec(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        codecs.put(type.id(), codec);
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> T decode(CustomPacketPayload.Type<T> type, FriendlyByteBuf buf) {
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec = (StreamCodec<? super RegistryFriendlyByteBuf, T>) codecs.get(type.id());
        return codec.decode(new RegistryFriendlyByteBuf(buf));
    }

    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> void encode(T payload, FriendlyByteBuf buf) {
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec = (StreamCodec<? super RegistryFriendlyByteBuf, T>) codecs.get(payload.type().id());
        codec.encode(new RegistryFriendlyByteBuf(buf), payload);
    }

    private FriendlyByteBuf encode(CustomPacketPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encode(payload, buf);
        return buf;
    }
    *///?}

    //? if forge {
    /*public <T extends CustomPacketPayload> void registerForge(Class<T> type, CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                               NetworkDirection direction, BiConsumer<T, NetworkEvent.Context> handler) {
        registerCodec(id, codec);
        CHANNEL.messageBuilder(type, nextMessageId++, direction)
                .encoder(this::encode)
                .decoder(buf -> decode(id, buf))
                .consumerMainThread((packet, context) -> handler.accept(packet, context.get()))
                .add();
    }
    *///?}

    public void sendToServer(CustomPacketPayload payload) {
        //? if fabric && >=1.21 {
        /*ClientPlayNetworking.send(payload);
        *///?} else if fabric {
        /*ClientPlayNetworking.send(payload.type().id(), encode(payload));
        *///?} else if forge {
        /*CHANNEL.sendToServer(payload);
        *///?} else if <26.1 {
        PacketDistributor.sendToServer(payload);
        //?} else {
        /*ClientPacketDistributor.sendToServer(payload);
        *///?}
    }

    public void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        //? if fabric && >=1.21 {
        /*ServerPlayNetworking.send(player, payload);
        *///?} else if fabric {
        /*ServerPlayNetworking.send(player, payload.type().id(), encode(payload));
        *///?} else if forge {
        /*if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
        *///?} else {
        if (player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
        //?}
    }
}

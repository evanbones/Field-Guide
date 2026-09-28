package com.evandev.fieldguide.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
*///?} else {
import net.neoforged.neoforge.network.PacketDistributor;
//?}

//? if neoforge && >=26.1 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

public class NetworkHelper {

    public void sendToServer(CustomPacketPayload payload) {
        //? if fabric {
        /*ClientPlayNetworking.send(payload);
        *///?} else if <26.1 {
        PacketDistributor.sendToServer(payload);
        //?} else {
        /*ClientPacketDistributor.sendToServer(payload);
        *///?}
    }

    public void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        //? if fabric {
        /*ServerPlayNetworking.send(player, payload);
        *///?} else {
        if (player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
        //?}
    }
}

package com.evandev.fieldguide;

import com.evandev.fieldguide.platform.Services;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ModDataComponents {

    public static final Supplier<DataComponentType<ResourceLocation>> ENTRY_ID = register("entry_id", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
    public static final Supplier<DataComponentType<Component>> ENTRY_NAME = register("entry_name", builder -> builder.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC));
    public static final Supplier<DataComponentType<String>> AUTHOR = register("author", builder -> builder.persistent(ExtraCodecs.PLAYER_NAME).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final Supplier<DataComponentType<List<String>>> VARIANTS = register("variants", builder -> builder.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)));
    public static final Supplier<DataComponentType<Map<String, String>>> CUSTOM_VARIANT_NAMES = register("custom_variant_names", builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING)).networkSynchronized(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)));
    public static final Supplier<DataComponentType<String>> CUSTOM_NAME = register("custom_name", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final Supplier<DataComponentType<String>> CUSTOM_DESCRIPTION = register("custom_description", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final Supplier<DataComponentType<String>> PHOTOGRAPH = register("photograph", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final Supplier<DataComponentType<Long>> DISCOVERY_TIME = register("discovery_time", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final Supplier<DataComponentType<Long>> DISCOVERY_GAME_TIME = register("discovery_game_time", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final Supplier<DataComponentType<Float>> DROP_CHANCE = register("drop_chance", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final Supplier<DataComponentType<Integer>> MIN_DROP = register("min_drop", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));
    public static final Supplier<DataComponentType<Integer>> MAX_DROP = register("max_drop", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static void init() {
    }

    private static <T> Supplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Services.REGISTRY.registerComponent(name, builderOperator);
    }
}

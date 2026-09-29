package com.evandev.fieldguide;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

//? if >=1.21 {
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
*///?}

public class ModDataComponents {

    //? if >=1.21 {
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
    //?} else {
    /*public static final Key<ResourceLocation> ENTRY_ID = new Key<>("entry_id", ResourceLocation.CODEC);
    public static final Key<Component> ENTRY_NAME = new Key<>("entry_name", Codec.STRING.xmap(Component.Serializer::fromJson, Component.Serializer::toJson));
    public static final Key<String> AUTHOR = new Key<>("author", Codec.STRING);
    public static final Key<List<String>> VARIANTS = new Key<>("variants", Codec.STRING.listOf());
    public static final Key<Map<String, String>> CUSTOM_VARIANT_NAMES = new Key<>("custom_variant_names", Codec.unboundedMap(Codec.STRING, Codec.STRING));
    public static final Key<String> CUSTOM_NAME = new Key<>("custom_name", Codec.STRING);
    public static final Key<String> CUSTOM_DESCRIPTION = new Key<>("custom_description", Codec.STRING);
    public static final Key<String> PHOTOGRAPH = new Key<>("photograph", Codec.STRING);
    public static final Key<Long> DISCOVERY_TIME = new Key<>("discovery_time", Codec.LONG);
    public static final Key<Long> DISCOVERY_GAME_TIME = new Key<>("discovery_game_time", Codec.LONG);
    public static final Key<Float> DROP_CHANCE = new Key<>("drop_chance", Codec.FLOAT);
    public static final Key<Integer> MIN_DROP = new Key<>("min_drop", Codec.INT);
    public static final Key<Integer> MAX_DROP = new Key<>("max_drop", Codec.INT);

    private static final String ROOT = Constants.MOD_ID;

    public record Key<T>(String name, Codec<T> codec) {
    }
    *///?}

    public static void init() {
    }

    //? if >=1.21 {
    private static <T> Supplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Services.REGISTRY.registerComponent(name, builderOperator);
    }

    public static <T> @Nullable T get(ItemStack stack, Supplier<DataComponentType<T>> type) {
        return stack.get(type.get());
    }

    public static <T> T getOrDefault(ItemStack stack, Supplier<DataComponentType<T>> type, T fallback) {
        return stack.getOrDefault(type.get(), fallback);
    }

    public static boolean has(ItemStack stack, Supplier<? extends DataComponentType<?>> type) {
        return stack.has(type.get());
    }

    public static <T> void set(ItemStack stack, Supplier<DataComponentType<T>> type, T value) {
        stack.set(type.get(), value);
    }

    public static void remove(ItemStack stack, Supplier<? extends DataComponentType<?>> type) {
        stack.remove(type.get());
    }
    //?} else {
    /*public static <T> @Nullable T get(ItemStack stack, Key<T> key) {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null || !root.contains(key.name())) return null;
        return key.codec().parse(NbtOps.INSTANCE, root.get(key.name())).result().orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, Key<T> key, T fallback) {
        T value = get(stack, key);
        return value != null ? value : fallback;
    }

    public static boolean has(ItemStack stack, Key<?> key) {
        CompoundTag root = stack.getTagElement(ROOT);
        return root != null && root.contains(key.name());
    }

    public static <T> void set(ItemStack stack, Key<T> key, T value) {
        if (value == null) {
            remove(stack, key);
            return;
        }
        Tag encoded = key.codec().encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
        if (encoded != null) stack.getOrCreateTagElement(ROOT).put(key.name(), encoded);
    }

    public static void remove(ItemStack stack, Key<?> key) {
        CompoundTag root = stack.getTagElement(ROOT);
        if (root == null) return;
        root.remove(key.name());
        // Keep stacks comparable with isSameItemSameTags once our data is gone
        if (root.isEmpty()) {
            stack.removeTagKey(ROOT);
            if (stack.getTag() != null && stack.getTag().isEmpty()) stack.setTag(null);
        }
    }
    *///?}
}

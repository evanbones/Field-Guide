package com.evandev.fieldguide.util;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class ByteBufCollections {
    private ByteBufCollections() {
    }

    public static <B extends FriendlyByteBuf, T> List<T> readList(B buf, Function<? super B, T> reader) {
        int size = buf.readVarInt();
        List<T> list = new ArrayList<>(Math.min(size, 65536));
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }
        return list;
    }

    public static <B extends FriendlyByteBuf, T> List<T> readList(B buf, int maxSize, Function<? super B, T> reader) {
        int size = buf.readVarInt();
        if (size > maxSize) {
            throw new DecoderException(size + " elements exceeded max size of: " + maxSize);
        }
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }
        return list;
    }

    public static <B extends FriendlyByteBuf, T> void writeCollection(B buf, Collection<T> values, BiConsumer<? super B, T> writer) {
        buf.writeVarInt(values.size());
        for (T value : values) {
            writer.accept(buf, value);
        }
    }

    public static <B extends FriendlyByteBuf, K, V> Map<K, V> readMap(B buf, Function<? super B, K> keyReader, Function<? super B, V> valueReader) {
        return readMap(buf, HashMap::new, keyReader, valueReader);
    }

    public static <B extends FriendlyByteBuf, K, V, M extends Map<K, V>> M readMap(B buf, IntFunction<M> mapFactory, Function<? super B, K> keyReader, Function<? super B, V> valueReader) {
        int size = buf.readVarInt();
        M map = mapFactory.apply(Math.min(size, 65536));
        for (int i = 0; i < size; i++) {
            map.put(keyReader.apply(buf), valueReader.apply(buf));
        }
        return map;
    }

    public static <B extends FriendlyByteBuf, K, V> void writeMap(B buf, Map<K, V> map, BiConsumer<? super B, K> keyWriter, BiConsumer<? super B, V> valueWriter) {
        buf.writeVarInt(map.size());
        map.forEach((key, value) -> {
            keyWriter.accept(buf, key);
            valueWriter.accept(buf, value);
        });
    }

    public static ItemStack readItem(FriendlyByteBuf buf) {
        //? if >=1.21 {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        //?} else {
        /*return buf.readItem();
        *///?}
    }

    public static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        //? if >=1.21 {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
        //?} else {
        /*buf.writeItem(stack);
        *///?}
    }
}

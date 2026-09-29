package com.evandev.fieldguide.network.legacy;

//? if <1.21 {
/*import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public final class ByteBufCodecs {
    public static final StreamCodec<FriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);
    public static final StreamCodec<FriendlyByteBuf, Component> COMPONENT = StreamCodec.of(FriendlyByteBuf::writeComponent, FriendlyByteBuf::readComponent);
    public static final StreamCodec<FriendlyByteBuf, String> STRING_UTF8 = StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf);

    private ByteBufCodecs() {
    }

    public static <B extends ByteBuf, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list() {
        return element -> StreamCodec.of(
                (buf, values) -> {
                    FriendlyByteBuf friendly = friendly(buf);
                    friendly.writeVarInt(values.size());
                    for (V value : values) element.encode(buf, value);
                },
                buf -> {
                    int size = friendly(buf).readVarInt();
                    List<V> values = new ArrayList<>(Math.min(size, 65536));
                    for (int i = 0; i < size; i++) values.add(element.decode(buf));
                    return values;
                }
        );
    }

    public static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return StreamCodec.of(
                (buf, value) -> friendly(buf).writeVarInt(toId.applyAsInt(value)),
                buf -> byId.apply(friendly(buf).readVarInt())
        );
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }
}
*///?}

package com.evandev.fieldguide.network.legacy;

//? if <1.21 {
/*import java.util.function.BiConsumer;
import java.util.function.Function;

public interface StreamCodec<B, V> {
    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(BiConsumer<V, B> encoder, Function<B, V> decoder) {
        return of((buf, value) -> encoder.accept(value, buf), decoder);
    }

    default <O> StreamCodec<B, O> apply(Function<StreamCodec<B, V>, StreamCodec<B, O>> operation) {
        return operation.apply(this);
    }
}
*///?}

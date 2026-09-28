package com.evandev.fieldguide.api;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;

import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

public record EntryUnlockData(
        boolean unlockedByDefault,
        List<ResourceLocation> prerequisites,
        List<UnlockTrigger> triggers,
        List<ResourceLocation> triggerOn
) {
    public static final EntryUnlockData DEFAULT = new EntryUnlockData(false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    public static final StreamCodec<RegistryFriendlyByteBuf, EntryUnlockData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.unlockedByDefault());
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, data.prerequisites());
                UnlockTrigger.CODEC.apply(ByteBufCodecs.list()).encode(buf, data.triggers());
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, data.triggerOn());
            },
            buf -> new EntryUnlockData(
                    buf.readBoolean(),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    UnlockTrigger.CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)
            )
    );

    public enum UnlockTrigger {
        KILL,
        SCAN,
        OBTAIN,
        EAT;

        private static final IntFunction<UnlockTrigger> BY_ID = ByIdMap.continuous(
                UnlockTrigger::ordinal,
                UnlockTrigger.values(),
                ByIdMap.OutOfBoundsStrategy.ZERO
        );

        public static final StreamCodec<ByteBuf, UnlockTrigger> CODEC = ByteBufCodecs.idMapper(
                BY_ID,
                UnlockTrigger::ordinal
        );
    }
}

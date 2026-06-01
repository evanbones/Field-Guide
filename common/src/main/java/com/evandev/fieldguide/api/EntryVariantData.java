package com.evandev.fieldguide.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record EntryVariantData(
        String variantId,
        Component displayName,
        DisplayType displayType,
        @Nullable StructureData structureData,
        @Nullable ResourceLocation displayId,
        @Nullable CompoundTag nbt,
        @Nullable ResourceLocation icon,
        List<String> components
) {
    public enum DisplayType {
        STRUCTURE,
        ENTITY,
        BLOCK,
        ITEM
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, EntryVariantData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeUtf(data.variantId());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, data.displayName());
                buf.writeEnum(data.displayType());
                buf.writeNullable(data.structureData(), (nb, sd) -> {
                    nb.writeNullable(sd.structureNbt(), FriendlyByteBuf::writeResourceLocation);
                    nb.writeCollection(sd.stackedBlocks() != null ? sd.stackedBlocks() : List.of(), FriendlyByteBuf::writeUtf);
                });
                buf.writeNullable(data.displayId(), FriendlyByteBuf::writeResourceLocation);
                buf.writeNullable(data.nbt(), (b, tag) -> b.writeNbt(tag));
                buf.writeNullable(data.icon(), FriendlyByteBuf::writeResourceLocation);
                buf.writeCollection(data.components(), FriendlyByteBuf::writeUtf);
            },
            buf -> new EntryVariantData(
                    buf.readUtf(),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    buf.readEnum(DisplayType.class),
                    buf.readNullable(nb -> new StructureData(
                            nb.readNullable(FriendlyByteBuf::readResourceLocation),
                            nb.readList(FriendlyByteBuf::readUtf)
                    )),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readNullable(b -> b.readNbt()),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readList(FriendlyByteBuf::readUtf)
            )
    );
}

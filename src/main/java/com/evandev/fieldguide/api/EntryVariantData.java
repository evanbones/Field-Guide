package com.evandev.fieldguide.api;

import com.evandev.fieldguide.util.ByteBufCollections;
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
    public static final StreamCodec<RegistryFriendlyByteBuf, EntryVariantData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeUtf(data.variantId());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, data.displayName());
                buf.writeEnum(data.displayType());
                buf.writeNullable(data.structureData(), (nb, sd) -> {
                    nb.writeNullable(sd.structureNbt(), FriendlyByteBuf::writeResourceLocation);
                    ByteBufCollections.writeCollection(nb, sd.stackedBlocks() != null ? sd.stackedBlocks() : List.of(), FriendlyByteBuf::writeUtf);
                });
                buf.writeNullable(data.displayId(), FriendlyByteBuf::writeResourceLocation);
                buf.writeNullable(data.nbt(), (b, tag) -> b.writeNbt(tag));
                buf.writeNullable(data.icon(), FriendlyByteBuf::writeResourceLocation);
                ByteBufCollections.writeCollection(buf, data.components(), FriendlyByteBuf::writeUtf);
            },
            buf -> new EntryVariantData(
                    buf.readUtf(),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    buf.readEnum(DisplayType.class),
                    buf.readNullable(nb -> new StructureData(
                            nb.readNullable(FriendlyByteBuf::readResourceLocation),
                            ByteBufCollections.readList(nb, FriendlyByteBuf::readUtf)
                    )),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readNullable(b -> b.readNbt()),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf)
            )
    );

    public boolean containsComponent(String targetId) {
        if (targetId == null) return false;
        if (displayId != null && targetId.equals(displayId.toString())) return true;
        if (components != null) {
            for (String comp : components) {
                int pipe = comp.indexOf('|');
                String id = pipe >= 0 ? comp.substring(0, pipe) : comp;
                if (targetId.equals(id)) return true;
            }
        }
        return false;
    }

    public boolean containsComponent(ResourceLocation targetId) {
        return targetId != null && containsComponent(targetId.toString());
    }

    public enum DisplayType {
        STRUCTURE,
        ENTITY,
        BLOCK,
        ITEM
    }
}

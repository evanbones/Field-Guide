package com.evandev.fieldguide.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record GuideEntry(
        ResourceLocation id,
        @Nullable ResourceLocation displayId,
        @Nullable ResourceLocation icon,
        EntryKind kind,
        boolean virtual,
        boolean autoPopulate,
        @Nullable String strategy,
        @Nullable List<ResourceLocation> childEntries,
        @Nullable StructureData structureData,
        @Nullable List<EntryVariantData> visualVariants,
        @Nullable VirtualData virtualData,
        EntryUnlockData unlockData
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideEntry> STREAM_CODEC = StreamCodec.of(
            (buf, entry) -> {
                buf.writeResourceLocation(entry.id());
                buf.writeNullable(entry.displayId(), FriendlyByteBuf::writeResourceLocation);
                buf.writeNullable(entry.icon(), FriendlyByteBuf::writeResourceLocation);
                buf.writeEnum(entry.kind());
                buf.writeBoolean(entry.virtual());
                buf.writeBoolean(entry.autoPopulate());
                buf.writeNullable(entry.strategy(), FriendlyByteBuf::writeUtf);
                buf.writeNullable(entry.childEntries(), (nb, comps) -> {
                    List<ResourceLocation> safeComps = comps.stream().filter(Objects::nonNull).toList();
                    nb.writeCollection(safeComps, FriendlyByteBuf::writeResourceLocation);
                });
                buf.writeNullable(entry.structureData(), (nb, data) -> {
                    nb.writeNullable(data.structureNbt(), FriendlyByteBuf::writeResourceLocation);
                    nb.writeCollection(data.stackedBlocks() != null ? data.stackedBlocks() : List.of(), FriendlyByteBuf::writeUtf);
                });
                buf.writeNullable(entry.visualVariants(), (nb, variants) -> {
                    nb.writeCollection(variants, (b, v) -> EntryVariantData.STREAM_CODEC.encode((RegistryFriendlyByteBuf) b, v));
                });
                buf.writeNullable(entry.virtualData(), (nb, data) -> {
                    nb.writeUtf(data.virtualType() != null ? data.virtualType() : "unknown");
                });
                EntryUnlockData safeUnlockData = entry.unlockData() != null ? entry.unlockData() : EntryUnlockData.DEFAULT;
                EntryUnlockData.STREAM_CODEC.encode(buf, safeUnlockData);
            },
            buf -> new GuideEntry(
                    buf.readResourceLocation(),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation),
                    buf.readEnum(EntryKind.class),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readNullable(nb -> nb.readList(FriendlyByteBuf::readResourceLocation)),
                    buf.readNullable(nb -> new StructureData(
                            nb.readNullable(FriendlyByteBuf::readResourceLocation),
                            nb.readList(FriendlyByteBuf::readUtf)
                    )),
                    buf.readNullable(nb -> nb.readList(b -> EntryVariantData.STREAM_CODEC.decode((RegistryFriendlyByteBuf) b))),
                    buf.readNullable(nb -> new VirtualData(nb.readUtf())),
                    EntryUnlockData.STREAM_CODEC.decode(buf)
            )
    );

    public boolean isComposite() {
        return childEntries != null && !childEntries.isEmpty();
    }

    public boolean isStructure() {
        return kind == EntryKind.STRUCTURE;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isAutoPopulate() {
        return autoPopulate;
    }

    public boolean hasVisualVariants() {
        return visualVariants != null && visualVariants.size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuideEntry that = (GuideEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
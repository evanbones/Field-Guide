package com.evandev.fieldguide.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CompositeDefinition(
        ResourceLocation id,
        @Nullable ResourceLocation displayId,
        @Nullable List<ResourceLocation> components,
        @Nullable ResourceLocation structureNbt,
        @Nullable List<String> stackedBlocks,
        @Nullable List<EntryVariantData> visualVariants
) {
}
package com.evandev.fieldguide.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StructureData(
        ResourceLocation structureNbt,
        List<String> stackedBlocks
) {
}

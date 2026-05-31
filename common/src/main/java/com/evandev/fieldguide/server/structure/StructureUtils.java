package com.evandev.fieldguide.server.structure;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.mixin.accessor.StructureTemplateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureUtils {
    public static Map<BlockPos, BlockState> getStructureBlocks(GuideEntry entry) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();

        if (entry.structureData() != null && entry.structureData().structureNbt() != null) {
            ResourceLocation nbtLocation = entry.structureData().structureNbt();
            ResourceLocation path = ResourceLocation.fromNamespaceAndPath(nbtLocation.getNamespace(), "structure/" + nbtLocation.getPath() + ".nbt");

            try {
                var res = Minecraft.getInstance().getResourceManager().getResource(path);
                if (res.isPresent()) {
                    CompoundTag tag = NbtIo.readCompressed(res.get().open(), NbtAccounter.unlimitedHeap());
                    StructureTemplate template = new StructureTemplate();
                    template.load(BuiltInRegistries.BLOCK.asLookup(), tag);

                    List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).getPalettes();

                    if (!palettes.isEmpty()) {
                        for (StructureTemplate.StructureBlockInfo info : palettes.getFirst().blocks()) {
                            blocks.put(info.pos(), info.state());
                        }
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load structure NBT: {}", path, e);
            }
        }

        return blocks;
    }

    public static Map<BlockPos, BlockState> getStackedBlocks(List<String> stackedBlocks) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        int defaultY = 0;

        for (String blockStr : stackedBlocks) {
            String[] parts = blockStr.split("\\|");
            BlockPos pos = new BlockPos(0, defaultY, 0);
            String blockIdPart = parts[0];
            int propIndex = 1;

            // Check if the first part is coordinates
            if (parts[0].contains(",")) {
                String[] coords = parts[0].split(",");
                if (coords.length == 3) {
                    pos = new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
                }
                blockIdPart = parts[1];
                propIndex = 2;
            }

            // Extract standard Minecraft bracket properties [key=value] if present
            String bracketProps = null;
            if (blockIdPart.contains("[")) {
                int bracketStart = blockIdPart.indexOf('[');
                int bracketEnd = blockIdPart.indexOf(']');
                if (bracketEnd > bracketStart) {
                    bracketProps = blockIdPart.substring(bracketStart + 1, bracketEnd);
                }
                blockIdPart = blockIdPart.substring(0, bracketStart);
            }

            ResourceLocation id = ResourceLocation.parse(blockIdPart);
            Block block = BuiltInRegistries.BLOCK.get(id);

            if (block != Blocks.AIR) {
                BlockState state = block.defaultBlockState();

                // Apply properties from brackets
                if (bracketProps != null) {
                    String[] props = bracketProps.split(",");
                    for (String propStr : props) {
                        String[] kv = propStr.split("=");
                        if (kv.length == 2) state = setProperty(state, kv[0], kv[1]);
                    }
                }

                // Apply properties from pipe notation
                if (parts.length > propIndex) {
                    String[] props = parts[propIndex].split(",");
                    for (String propStr : props) {
                        String[] kv = propStr.split("=");
                        if (kv.length == 2) state = setProperty(state, kv[0], kv[1]);
                    }
                }
                blocks.put(pos, state);
            }

            if (!parts[0].contains(",")) defaultY++;
        }
        return blocks;
    }

    private static BlockState setProperty(BlockState state, String key, String value) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals(key)) {
                return parseAndSet(state, prop, value);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState parseAndSet(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(state);
    }
}
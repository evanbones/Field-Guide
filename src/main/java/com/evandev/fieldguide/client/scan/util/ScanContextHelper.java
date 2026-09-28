package com.evandev.fieldguide.client.scan.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class ScanContextHelper {

    private ScanContextHelper() {
    }

    /**
     * Traces vertically up a trunk/tree column up to maxSteps blocks, stopping when non-tree blocks or air are reached.
     */
    public static BlockPos traceTreeCanopy(Level level, BlockPos basePos, int maxSteps, Predicate<ResourceLocation> isTreeComponent) {
        if (level == null || basePos == null) return basePos;
        BlockPos canopyPos = basePos;
        BlockPos.MutableBlockPos cursor = basePos.mutable();

        for (int i = 0; i < maxSteps; i++) {
            cursor.move(Direction.UP);
            BlockState upState = level.getBlockState(cursor);
            if (upState.isAir()) break;

            ResourceLocation upId = BuiltInRegistries.BLOCK.getKey(upState.getBlock());
            boolean isTreePart = upState.is(BlockTags.LOGS) || upState.is(BlockTags.LEAVES) || (isTreeComponent != null && isTreeComponent.test(upId));

            if (isTreePart) {
                canopyPos = cursor.immutable();
            } else {
                break;
            }
        }
        return canopyPos;
    }

    /**
     * Samples nearby non-air blocks around sample centers (e.g. hit position and canopy position).
     */
    public static void sampleNearbyBlocks(Level level, BlockPos hitPos, BlockPos canopyPos, BiConsumer<BlockPos, BlockState> visitor) {
        if (level == null || hitPos == null) return;
        Set<BlockPos> checked = new HashSet<>();
        List<BlockPos> sampleCenters = (canopyPos != null && !canopyPos.equals(hitPos)) ? List.of(hitPos, canopyPos) : List.of(hitPos);

        for (BlockPos center : sampleCenters) {
            boolean isCanopy = center.equals(canopyPos) && !canopyPos.equals(hitPos);
            int rXZ = isCanopy ? 6 : 4;
            int rY = 4;

            for (int dx = -rXZ; dx <= rXZ; dx++) {
                for (int dy = -rY; dy <= rY; dy++) {
                    for (int dz = -rXZ; dz <= rXZ; dz++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (!checked.add(p) || p.equals(hitPos)) continue;

                        BlockState state = level.getBlockState(p);
                        if (state.isAir()) continue;

                        visitor.accept(p, state);
                    }
                }
            }
        }
    }
}

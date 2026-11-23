package com.evandev.fieldguide.client.render;

//? if <26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public final class GuideBlockTint {
    private static int depth;

    public static void begin() {
        depth++;
    }

    public static void end() {
        depth = Math.max(0, depth - 1);
    }

    public static boolean isActive() {
        return depth > 0;
    }

    @Nullable
    public static BlockAndTintGetter levelFor(BlockState state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        Biome biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.PLAINS);
        if (biome == null) return null;
        return new DefaultBiomeGetter(level, biome, state);
    }

    private record DefaultBiomeGetter(ClientLevel level, Biome biome, BlockState state) implements BlockAndTintGetter {

        @Override
        public float getShade(Direction direction, boolean shade) {
            return level.getShade(direction, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return level.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return resolver.getColor(biome, pos.getX(), pos.getZ());
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(BlockPos.ZERO) ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }
    }
}
//?}

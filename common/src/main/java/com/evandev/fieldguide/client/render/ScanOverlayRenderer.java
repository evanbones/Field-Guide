package com.evandev.fieldguide.client.render;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.scan.FieldGuideScanner;
import com.evandev.fieldguide.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.awt.*;
import java.util.*;
import java.util.Queue;

public class ScanOverlayRenderer {
    private static final float VERTICAL_BUFFER = 1.3f;

    public static int getPackedScanLightCoords(float limitY, float r, float g, float b, float a) {
        short encodedLimit = (short) Mth.clamp(limitY * 100.0f, -32000, 32000);
        int r4 = (int) (r * 15);
        int g4 = (int) (g * 15);
        int b4 = (int) (b * 15);
        int a4 = (int) (a * 15);
        short colorPacked = (short) ((r4 << 12) | (g4 << 8) | (b4 << 4) | a4);
        return (encodedLimit & 0xFFFF) | ((colorPacked & 0xFFFF) << 16);
    }

    public static void render(PoseStack poseStack, float partialTick, Camera camera, SubmitNodeCollector collector) {
        FieldGuideScanner scanner = FieldGuideScanner.getInstance();
        Minecraft mc = Minecraft.getInstance();

        Entity outOfRangeEntity = resolveEntity(scanner.getOutOfRangeEntity());
        Entity scanningRaw = scanner.getScanningEntity() != null ? scanner.getScanningEntity() : (scanner.getFadingEntity() != null ? scanner.getFadingEntity() : scanner.getOutOfRangeEntity());
        Entity targetEntity = resolveEntity(scanningRaw);

        BlockPos outOfRangePos = scanner.getOutOfRangePos();
        BlockPos targetBlock = (scanner.getScanningTarget() instanceof Block && scanner.getScanningPos() != null) ? scanner.getScanningPos() : (scanner.getFadingPos() != null ? scanner.getFadingPos() : outOfRangePos);

        if (targetEntity == null && targetBlock == null) return;

        Vec3 camPos = camera.position();
        float red, green, blue, alpha;

        if ((outOfRangeEntity != null && targetEntity == outOfRangeEntity) || (outOfRangePos != null && targetBlock == outOfRangePos)) {
            if (!ClientConfig.get().showOutOfRangeOverlay) return;
            red = 1.0F;
            green = 0.0F;
            blue = 0.0F;
            alpha = 0.0F + ((float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5) * 0.2F);
        } else {
            Color c = new Color(ClientConfig.get().getScanOverlayColorInt());
            red = c.getRed() / 255.0F;
            green = c.getGreen() / 255.0F;
            blue = c.getBlue() / 255.0F;
            alpha = (float) (scanner.getScanningEntity() != null || scanner.getScanningTarget() != null ? ClientConfig.get().scanOverlayAlpha : ClientConfig.get().scanOverlayAlpha * scanner.getFadeProgress(partialTick));
        }

        if (targetBlock != null && alpha > 0.01f) {
            renderBlockOverlay(poseStack, partialTick, camPos, collector, targetBlock, scanner, mc, red, green, blue, alpha);
        }

        if (targetEntity != null && alpha > 0.01f) {
            renderEntityOverlay(poseStack, partialTick, camPos, collector, targetEntity, outOfRangeEntity, scanner, mc, red, green, blue, alpha);
        }
    }

    private static Entity resolveEntity(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof EnderDragonPart dragonPart) return dragonPart.parentMob;
        try {
            return (Entity) entity.getClass().getMethod("getParent").invoke(entity);
        } catch (Exception ignored) {
            return entity;
        }
    }

    private static void renderBlockOverlay(PoseStack poseStack, float partialTick, Vec3 camPos, SubmitNodeCollector collector, BlockPos targetBlock, FieldGuideScanner scanner, Minecraft mc, float red, float green, float blue, float alpha) {
        boolean isOutOfRange = scanner.getOutOfRangePos() != null && targetBlock == scanner.getOutOfRangePos();
        float progress = isOutOfRange ? 1.0f : (scanner.getScanningTarget() != null ? scanner.getScanProgress(partialTick) : scanner.getFadeProgress(partialTick));
        if (progress <= 0.0f) return;

        float fillHeight = scanner.getScanningTarget() != null ? progress : 1.0f;
        BlockState targetState = Objects.requireNonNull(mc.level).getBlockState(targetBlock);
        Block targetBlockType = targetState.getBlock();
        Object entry = ClientFieldGuideManager.getInstance().getEntryForTarget(targetBlockType);

        Set<BlockPos> blocksToRender = new HashSet<>();

        if (targetState.is(BlockTags.LOGS) || targetState.is(BlockTags.LEAVES)) {
            blocksToRender = gatherTreeBlocks(mc, targetBlock);
        } else {
            blocksToRender.add(targetBlock);

            if (entry instanceof GuideEntry composite && composite.isComposite()) {
                Queue<BlockPos> queue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distSqr(targetBlock)));
                queue.add(targetBlock);
                int maxBlocks = 400;

                while (!queue.isEmpty() && blocksToRender.size() < maxBlocks) {
                    BlockPos pos = queue.poll();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                BlockPos neighbor = pos.offset(dx, dy, dz);
                                int hDist = Math.max(Math.abs(neighbor.getX() - targetBlock.getX()), Math.abs(neighbor.getZ() - targetBlock.getZ()));
                                int vDist = Math.abs(neighbor.getY() - targetBlock.getY());
                                if (hDist > 6 || vDist > 32) continue;

                                boolean isHorizontal = dx != 0 || dz != 0;
                                Identifier neighborId = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(neighbor).getBlock());

                                if (isHorizontal && (composite.childEntries() == null || !composite.childEntries().contains(neighborId)))
                                    continue;

                                if (!blocksToRender.contains(neighbor)) {
                                    if ((composite.childEntries() != null && composite.childEntries().contains(neighborId)) || (composite.displayId() != null && composite.displayId().equals(neighborId))) {
                                        blocksToRender.add(neighbor);
                                        queue.add(neighbor);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (targetState.is(ModTags.Blocks.MULTIBLOCK_SCAN)) {
                Queue<BlockPos> queue = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distSqr(targetBlock)));
                queue.add(targetBlock);
                int maxBlocks = 150;

                while (!queue.isEmpty() && blocksToRender.size() < maxBlocks) {
                    BlockPos pos = queue.poll();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                BlockPos neighbor = pos.offset(dx, dy, dz);
                                int hDist = Math.max(Math.abs(neighbor.getX() - targetBlock.getX()), Math.abs(neighbor.getZ() - targetBlock.getZ()));
                                int vDist = Math.abs(neighbor.getY() - targetBlock.getY());
                                if (hDist > 5 || vDist > 32) continue;

                                if (!blocksToRender.contains(neighbor)) {
                                    Block neighborBlock = mc.level.getBlockState(neighbor).getBlock();
                                    if (neighborBlock == targetBlockType) {
                                        blocksToRender.add(neighbor);
                                        queue.add(neighbor);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        int minY = Integer.MAX_VALUE;
        double maxAbsoluteY = Integer.MIN_VALUE;

        for (BlockPos pos : blocksToRender) {
            minY = Math.min(minY, pos.getY());
            BlockState state = mc.level.getBlockState(pos);
            double shapeHeight = (mc.player != null && !state.isAir() && !state.isCollisionShapeFullBlock(mc.level, pos)) ? Math.max(1.0, state.getShape(mc.level, pos, CollisionContext.of(mc.player)).max(Direction.Axis.Y)) : 1.0;
            maxAbsoluteY = Math.max(maxAbsoluteY, pos.getY() + shapeHeight);
        }

        float globalScanLimitY = fillHeight >= 1.0f ? 10000.0f : (float) (minY + ((maxAbsoluteY - minY) * fillHeight));

        BlockModelRenderState blockRenderState = new BlockModelRenderState();
        BlockModelResolver resolver = new BlockModelResolver(mc.getModelManager());

        for (BlockPos pos : blocksToRender) {
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir() && state.getRenderShape() == RenderShape.MODEL) {
                Vec3 offset = state.getOffset(pos);
                double x = pos.getX() - camPos.x + offset.x;
                double y = pos.getY() - camPos.y + offset.y;
                double z = pos.getZ() - camPos.z + offset.z;

                float localScanLimitY = fillHeight >= 1.0f ? 10000.0f : (globalScanLimitY - (float) camPos.y);

                SubmitNodeCollector depthCollector = new ScanNodeCollector(collector, 1f, 1f, 1f, 0f, localScanLimitY, true);
                SubmitNodeCollector tintedCollector = new ScanNodeCollector(collector, red, green, blue, alpha, localScanLimitY, false);

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                resolver.update(blockRenderState, state, BlockDisplayContext.create());

                if (!blockRenderState.tintLayers().isEmpty()) {
                    for (int i = 0; i < blockRenderState.tintLayers().size(); i++) {
                        blockRenderState.tintLayers().set(i, -1);
                    }
                }

                blockRenderState.submit(poseStack, depthCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                blockRenderState.submit(poseStack, tintedCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    private static Set<BlockPos> gatherTreeBlocks(Minecraft mc, BlockPos startPos) {
        Set<BlockPos> blocks = new HashSet<>();
        if (mc.level == null) return blocks;

        BlockState startState = mc.level.getBlockState(startPos);
        BlockPos trunkStart = startPos;

        if (startState.is(BlockTags.LEAVES)) {
            Queue<BlockPos> queue = new LinkedList<>();
            Set<BlockPos> visited = new HashSet<>();
            queue.add(startPos);
            visited.add(startPos);

            while (!queue.isEmpty() && visited.size() < 128) {
                BlockPos pos = queue.poll();
                if (mc.level.getBlockState(pos).is(BlockTags.LOGS)) {
                    trunkStart = pos;
                    break;
                }
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        BlockState neighborState = mc.level.getBlockState(neighbor);
                        if (neighborState.is(BlockTags.LEAVES) || neighborState.is(BlockTags.LOGS)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        Set<BlockPos> trunk = new HashSet<>();
        Queue<BlockPos> trunkQueue = new LinkedList<>();
        trunkQueue.add(trunkStart);
        trunk.add(trunkStart);

        while (!trunkQueue.isEmpty() && trunk.size() < 256) {
            BlockPos pos = trunkQueue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.offset(dx, dy, dz);

                        int hDist = Math.max(Math.abs(neighbor.getX() - trunkStart.getX()), Math.abs(neighbor.getZ() - trunkStart.getZ()));
                        if (hDist <= 2 && !trunk.contains(neighbor) && mc.level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            trunk.add(neighbor);
                            trunkQueue.add(neighbor);
                        }
                    }
                }
            }
        }

        Set<BlockPos> leaves = new HashSet<>();
        Map<BlockPos, Integer> leafDistances = new HashMap<>();
        Queue<BlockPos> leafQueue = new LinkedList<>();

        for (BlockPos logPos : trunk) {
            leafQueue.add(logPos);
            leafDistances.put(logPos, 0);
        }

        while (!leafQueue.isEmpty() && leaves.size() < 600) {
            BlockPos pos = leafQueue.poll();
            int currentDist = leafDistances.get(pos);
            if (currentDist >= 5) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (!leafDistances.containsKey(neighbor)) {
                    BlockState neighborState = mc.level.getBlockState(neighbor);
                    if (neighborState.is(BlockTags.LEAVES)) {
                        leafDistances.put(neighbor, currentDist + 1);
                        leaves.add(neighbor);
                        leafQueue.add(neighbor);
                    }
                }
            }
        }

        blocks.addAll(trunk);
        blocks.addAll(leaves);
        blocks.add(startPos);
        return blocks;
    }

    private static void renderEntityOverlay(PoseStack poseStack, float partialTick, Vec3 camPos, SubmitNodeCollector collector, Entity targetEntity, Entity outOfRangeEntity, FieldGuideScanner scanner, Minecraft mc, float red, float green, float blue, float alpha) {
        float fillHeight = (outOfRangeEntity != null || scanner.getScanningEntity() == null) ? 1.0f : scanner.getScanProgress(partialTick);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(targetEntity, partialTick);

        float globalEntityLimitY = (float) state.y + (targetEntity.getBbHeight() * fillHeight * VERTICAL_BUFFER);
        float localScanLimitY = fillHeight >= 1.0f ? 10000.0f : (globalEntityLimitY - (float) camPos.y);

        SubmitNodeCollector depthCollector = new ScanNodeCollector(collector, 1f, 1f, 1f, 0f, localScanLimitY, true);
        SubmitNodeCollector forcedCollector = new ScanNodeCollector(collector, red, green, blue, alpha, localScanLimitY, false);

        CameraRenderState cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;

        double x = state.x - camPos.x;
        double y = state.y - camPos.y;
        double z = state.z - camPos.z;

        dispatcher.submit(state, cameraState, x, y, z, poseStack, depthCollector);
        dispatcher.submit(state, cameraState, x, y, z, poseStack, forcedCollector);
    }
}
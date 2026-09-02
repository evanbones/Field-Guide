package com.evandev.fieldguide.client.render;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.ModRenderTypes;
import com.evandev.fieldguide.client.scan.FieldGuideScanner;
import com.evandev.fieldguide.compat.etf.EtfCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.platform.Services;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.Queue;

public class ScanOverlayRenderer {
    protected static final VertexConsumer DUMMY_CONSUMER = new VertexConsumer() {
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public @NotNull VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    };
    private static final float VERTICAL_BUFFER = 1.3f;
    private static final Map<RenderType, Boolean> GLINT_CACHE = new IdentityHashMap<>();
    private static Boolean etfLoaded;

    public static void render(PoseStack poseStack, float partialTick, Camera camera, MultiBufferSource.BufferSource bufferSource) {
        FieldGuideScanner scanner = FieldGuideScanner.getInstance();
        Minecraft mc = Minecraft.getInstance();

        Entity outOfRangeEntity = resolveEntity(scanner.getOutOfRangeEntity());
        Entity scanningRaw = scanner.getScanningEntity() != null ? scanner.getScanningEntity() : (scanner.getFadingEntity() != null ? scanner.getFadingEntity() : scanner.getOutOfRangeEntity());
        Entity targetEntity = resolveEntity(scanningRaw);

        BlockPos outOfRangePos = scanner.getOutOfRangePos();
        BlockPos targetBlock = (scanner.getScanningTarget() instanceof Block && scanner.getScanningPos() != null) ? scanner.getScanningPos() : (scanner.getFadingPos() != null ? scanner.getFadingPos() : outOfRangePos);

        if (targetEntity == null && targetBlock == null) return;
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        Vec3 camPos = camera.getPosition();
        float red, green, blue, alpha;

        if ((outOfRangeEntity != null && targetEntity == outOfRangeEntity) || (outOfRangePos != null && targetBlock == outOfRangePos)) {
            if (!ClientConfig.get().showOutOfRangeOverlay) {
                return;
            }

            red = 1.0F;
            green = 0.0F;
            blue = 0.0F;
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            alpha = 0.0F + (pulse * 0.2F);
        } else {
            int colorInt = ClientConfig.get().getScanOverlayColorInt();
            Color c = new Color(colorInt);
            red = c.getRed() / 255.0F;
            green = c.getGreen() / 255.0F;
            blue = c.getBlue() / 255.0F;
            alpha = (float) (scanner.getScanningEntity() != null || scanner.getScanningTarget() != null
                    ? ClientConfig.get().scanOverlayAlpha
                    : ClientConfig.get().scanOverlayAlpha * scanner.getFadeProgress(partialTick));
        }

        if (targetBlock != null && alpha > 0.01f) {
            renderBlockOverlay(poseStack, partialTick, camPos, bufferSource, targetBlock, scanner, mc, red, green, blue, alpha);
        }

        if (targetEntity != null && alpha > 0.01f) {
            renderEntityOverlay(poseStack, partialTick, camPos, bufferSource, targetEntity, outOfRangeEntity, scanner, mc, red, green, blue, alpha);
        }
    }

    protected static Entity resolveEntity(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof EnderDragonPart dragonPart) {
            return dragonPart.parentMob;
        }
        try {
            java.lang.reflect.Method getParent = entity.getClass().getMethod("getParent");
            Object parent = getParent.invoke(entity);
            if (parent instanceof Entity parentEntity) {
                return parentEntity;
            }
        } catch (Exception ignored) {
        }
        return entity;
    }

    protected static VertexConsumer createTintedConsumer(VertexConsumer delegate, MultiBufferSource provider, float r, float g, float b, float a) {
        if (isEtfLoaded()) {
            return EtfCompat.createTintedConsumer(delegate, provider, r, g, b, a);
        }
        return new TintedVertexConsumer(delegate, r, g, b, a);
    }

    private static boolean isGlint(RenderType type) {
        Boolean cached = GLINT_CACHE.get(type);
        if (cached == null) {
            cached = type.toString().contains("glint");
            GLINT_CACHE.put(type, cached);
        }
        return cached;
    }

    protected static boolean isEtfLoaded() {
        if (etfLoaded == null) {
            etfLoaded = Services.PLATFORM.isModLoaded("entity_texture_features");
        }
        return etfLoaded;
    }

    public static VertexConsumer getScanBuffer(MultiBufferSource.BufferSource source, RenderType type) {
        if (!isEtfLoaded()) {
            return source.getBuffer(type);
        }
        EtfCompat.pushPreventRenderLayerTextureModify();
        try {
            return source.getBuffer(type);
        } finally {
            EtfCompat.popRenderLayerTextureModify();
        }
    }

    private static void renderBlockOverlay(PoseStack poseStack, float partialTick, Vec3 camPos, MultiBufferSource.BufferSource bufferSource, BlockPos targetBlock, FieldGuideScanner scanner, Minecraft mc, float red, float green, float blue, float alpha) {
        boolean isOutOfRange = scanner.getOutOfRangePos() != null && targetBlock == scanner.getOutOfRangePos();
        float progress = isOutOfRange ? 1.0f : (scanner.getScanningTarget() != null ? scanner.getScanProgress(partialTick) : scanner.getFadeProgress(partialTick));
        if (progress <= 0.0f) return;

        float fillHeight = scanner.getScanningTarget() != null ? progress : 1.0f;

        BlockState targetState = mc.level.getBlockState(targetBlock);
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
                                ResourceLocation neighborId = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(neighbor).getBlock());

                                if (isHorizontal && (composite.childEntries() == null || !composite.childEntries().contains(neighborId)))
                                    continue;


                                if (!blocksToRender.contains(neighbor)) {
                                    if ((composite.childEntries() != null && composite.childEntries().contains(neighborId)) ||
                                            (composite.displayId() != null && composite.displayId().equals(neighborId))) {
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
            double shapeHeight = 1.0;
            if (mc.player != null && !state.isAir()) {
                shapeHeight = state.isCollisionShapeFullBlock(mc.level, pos)
                        ? 1.0
                        : Math.max(1.0, state.getShape(mc.level, pos, CollisionContext.of(mc.player)).max(Direction.Axis.Y));
            }
            maxAbsoluteY = Math.max(maxAbsoluteY, pos.getY() + shapeHeight);
        }

        double totalHeight = maxAbsoluteY - minY;
        double globalScanLimitY = fillHeight >= 1.0f ? 10000.0 : (minY + (totalHeight * fillHeight));

        MultiBufferSource depthSource = new ScanBufferSourceWrapper(bufferSource, 1, 1, 1, 1, true, false);

        for (BlockPos pos : blocksToRender) {
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir()) {
                Vec3 offset = state.getOffset(mc.level, pos);
                double x = pos.getX() - camPos.x + offset.x;
                double y = pos.getY() - camPos.y + offset.y;
                double z = pos.getZ() - camPos.z + offset.z;

                poseStack.pushPose();
                poseStack.translate(x, y, z);

                float localScanLimitY = fillHeight >= 1.0f ? 10000.0f : (float) (globalScanLimitY - pos.getY());

                if (ModRenderTypes.SCAN_BLOCK_SHADER != null) {
                    ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ScanLimitY").set(localScanLimitY);
                    Matrix4f modelViewMat = new Matrix4f(poseStack.last().pose());
                    ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("InverseModelViewMat").set(modelViewMat.invert());
                    if (ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator") != null) {
                        ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }

                if (state.getRenderShape() == RenderShape.MODEL) {
                    RenderType type = ItemBlockRenderTypes.getRenderType(state, false);
                    renderBlockModelAsShell(mc, state, pos, poseStack, depthSource.getBuffer(type), blocksToRender);
                } else {
                    mc.getBlockRenderer().renderSingleBlock(state, poseStack, depthSource, 15728880, OverlayTexture.pack(0, 10));
                }

                poseStack.popPose();
                bufferSource.endBatch();
            }
        }

        MultiBufferSource tintedSource = new ScanBufferSourceWrapper(bufferSource, red, green, blue, alpha, false, false);

        for (BlockPos pos : blocksToRender) {
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir()) {
                Vec3 offset = state.getOffset(mc.level, pos);
                double x = pos.getX() - camPos.x + offset.x;
                double y = pos.getY() - camPos.y + offset.y;
                double z = pos.getZ() - camPos.z + offset.z;

                poseStack.pushPose();
                poseStack.translate(x, y, z);

                float localScanLimitY = fillHeight >= 1.0f ? 10000.0f : (float) (globalScanLimitY - pos.getY());

                if (ModRenderTypes.SCAN_BLOCK_SHADER != null) {
                    ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ScanLimitY").set(localScanLimitY);
                    Matrix4f modelViewMat = new Matrix4f(poseStack.last().pose());
                    ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("InverseModelViewMat").set(modelViewMat.invert());
                    if (ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator") != null) {
                        ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator").set(red, green, blue, alpha);
                    }
                }

                if (state.getRenderShape() == RenderShape.MODEL) {
                    RenderType typeColor = ItemBlockRenderTypes.getRenderType(state, false);
                    renderBlockModelAsShell(mc, state, pos, poseStack, tintedSource.getBuffer(typeColor), blocksToRender);
                } else {
                    mc.getBlockRenderer().renderSingleBlock(state, poseStack, tintedSource, 15728880, OverlayTexture.pack(0, 10));
                }

                poseStack.popPose();
                bufferSource.endBatch();
            }
        }
    }

    private static Set<BlockPos> gatherTreeBlocks(Minecraft mc, BlockPos startPos) {
        Set<BlockPos> blocks = new HashSet<>();
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

    protected static void renderBlockModelAsShell(Minecraft mc, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, Set<BlockPos> blocksToRender) {
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create();
        long seed = state.getSeed(pos);
        PoseStack.Pose pose = poseStack.last();

        for (Direction dir : Direction.values()) {
            if (blocksToRender.contains(pos.relative(dir))) {
                BlockState neighborState = mc.level.getBlockState(pos.relative(dir));
                if (neighborState.isCollisionShapeFullBlock(mc.level, pos.relative(dir))) {
                    continue;
                }
            }
            random.setSeed(seed);
            for (BakedQuad quad : model.getQuads(state, dir, random)) {
                consumer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, 15728880, OverlayTexture.pack(0, 10));
            }
        }
        random.setSeed(seed);
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            consumer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, 15728880, OverlayTexture.pack(0, 10));
        }
    }

    private static void renderEntityOverlay(PoseStack poseStack, float partialTick, Vec3 camPos, MultiBufferSource.BufferSource bufferSource, Entity targetEntity, Entity outOfRangeEntity, FieldGuideScanner scanner, Minecraft mc, float red, float green, float blue, float alpha) {
        float fillHeight = (outOfRangeEntity != null || scanner.getScanningEntity() == null) ? 1.0f : scanner.getScanProgress(partialTick);

        double x = Mth.lerp(partialTick, targetEntity.xOld, targetEntity.getX()) - camPos.x;
        double y = Mth.lerp(partialTick, targetEntity.yOld, targetEntity.getY()) - camPos.y;
        double z = Mth.lerp(partialTick, targetEntity.zOld, targetEntity.getZ()) - camPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        double entityHeight = targetEntity.getBbHeight();
        float localScanLimitY = fillHeight >= 1.0f ? 10000.0f : (float) (entityHeight * fillHeight * VERTICAL_BUFFER);

        if (ModRenderTypes.SCAN_ENTITY_SHADER != null) {
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanLimitY").set(localScanLimitY);
            Matrix4f modelViewMat = new Matrix4f(poseStack.last().pose());
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("InverseModelViewMat").set(modelViewMat.invert());
            if (ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
                ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        float yaw = Mth.lerp(partialTick, targetEntity.yRotO, targetEntity.getYRot());

        MultiBufferSource depthSource = new ScanBufferSourceWrapper(bufferSource, 1, 1, 1, 1, true, true);
        mc.getEntityRenderDispatcher().render(targetEntity, 0.0D, 0.0D, 0.0D, yaw, partialTick, poseStack, depthSource, 15728880);
        bufferSource.endBatch();

        if (ModRenderTypes.SCAN_ENTITY_SHADER != null && ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(red, green, blue, alpha);
        }

        MultiBufferSource forcedSource = new ScanBufferSourceWrapper(bufferSource, red, green, blue, alpha, false, true);
        mc.getEntityRenderDispatcher().render(targetEntity, 0.0D, 0.0D, 0.0D, yaw, partialTick, poseStack, forcedSource, 15728880);
        bufferSource.endBatch();

        poseStack.popPose();
    }

    protected static class ScanBufferSourceWrapper implements MultiBufferSource {
        private final MultiBufferSource.BufferSource delegate;
        private final float r, g, b, a;
        private final boolean isDepth;
        private final boolean isEntity;

        public ScanBufferSourceWrapper(MultiBufferSource.BufferSource delegate, float r, float g, float b, float a, boolean isDepth, boolean isEntity) {
            this.delegate = delegate;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.isDepth = isDepth;
            this.isEntity = isEntity;
        }

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType type) {
            if (isGlint(type)) {
                return DUMMY_CONSUMER;
            }

            RenderType wrappedType = isDepth
                    ? ModRenderTypes.wrapForDepth(type, isEntity)
                    : ModRenderTypes.wrapForScan(type, isEntity);

            return createTintedConsumer(getScanBuffer(delegate, wrappedType), this, r, g, b, a);
        }
    }
}
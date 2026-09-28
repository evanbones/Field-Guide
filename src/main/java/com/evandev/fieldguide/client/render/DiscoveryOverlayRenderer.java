package com.evandev.fieldguide.client.render;

import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.client.scan.FieldGuideScanner;
import com.evandev.fieldguide.client.scan.manager.FieldGuideRaytracer;
import com.evandev.fieldguide.client.scan.manager.FieldGuideScanManager;
import com.evandev.fieldguide.config.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

//? if <26.1 {
import com.evandev.fieldguide.client.ModRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
//?}

//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
*///?}

//? if >=26.3 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

import java.util.Optional;
import java.util.OptionalDouble;
*///?}

public final class DiscoveryOverlayRenderer {

    private static final int RECOMPUTE_INTERVAL = 10;
    private static final int MAX_FOUND = 8000;
    private static final int MAX_RENDERED = 1500;
    private static final float FULL_FILL = 10000.0F;
    private static final float SUBTLE_FACTOR = 0.55F;
    private static final float PULSE_MIN = 0.4F;
    private static final double PULSE_SPEED = 350.0;
    private static final float FADE_MS = 400.0F;

    private static final double[][] SAMPLE_OFFSETS = {
            {0.5, 0.5, 0.5},
            {0.1, 0.1, 0.1}, {0.9, 0.1, 0.1}, {0.1, 0.9, 0.1}, {0.9, 0.9, 0.1},
            {0.1, 0.1, 0.9}, {0.9, 0.1, 0.9}, {0.1, 0.9, 0.9}, {0.9, 0.9, 0.9}
    };

    private static final List<BlockPos> blockTargets = new ArrayList<>();
    private static final Map<BlockPos, Float> blockFade = new HashMap<>();
    private static final Map<Entity, Float> entityFade = new HashMap<>();
    private static Level cachedLevel;
    private static long lastRecomputeTime = Long.MIN_VALUE;
    private static long lastRenderMs = 0L;

    private DiscoveryOverlayRenderer() {
    }

    //? if <26.1 {
    public static void render(PoseStack poseStack, float partialTick, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource) {
    //?} else {
    /*public static void render(float partialTick, CameraRenderState cameraState, double clearDepth) {
    *///?}
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            blockFade.clear();
            entityFade.clear();
            return;
        }

        //? if >=26.1 {
        /*Frustum frustum = cameraState.cullFrustum;
        *///?}
        double activeRange = FieldGuideScanManager.getInstance().getDiscoveryDistance(mc);
        //? if <26.1 {
        boolean active = activeRange > 0;
        //?} else {
        /*boolean active = activeRange > 0 && frustum != null;
        *///?}

        Set<BlockPos> currentBlocks;
        Set<Entity> currentEntities;
        if (active) {
            refreshBlockTargets(mc, player, activeRange, frustum);
            FieldGuideScanner scanner = FieldGuideScanner.getInstance();
            BlockPos activePos = scanner.getScanningPos() != null ? scanner.getScanningPos() : scanner.getFadingPos();
            Entity activeEntity = scanner.getScanningEntity() != null ? scanner.getScanningEntity() : scanner.getFadingEntity();
            currentBlocks = new HashSet<>(blockTargets);
            if (activePos != null) {
                currentBlocks.remove(activePos);
            }
            currentEntities = new HashSet<>(gatherEntityTargets(mc, player, activeRange, activeEntity, frustum));
        } else {
            currentBlocks = Collections.emptySet();
            currentEntities = Collections.emptySet();
        }

        long nowMs = System.currentTimeMillis();
        float step = lastRenderMs == 0L ? 0.0F : Math.min(1.0F, (nowMs - lastRenderMs) / FADE_MS);
        lastRenderMs = nowMs;
        advanceFade(blockFade, currentBlocks, step);
        advanceEntityFade(entityFade, currentEntities, step);

        if (blockFade.isEmpty() && entityFade.isEmpty()) {
            return;
        }

        int color = ClientConfig.get().getScanOverlayColorInt();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        double base = ClientConfig.get().scanOverlayAlpha;
        float wave = (float) (0.5 + 0.5 * Math.sin(nowMs / PULSE_SPEED));
        float pulsed = (float) (base * base * SUBTLE_FACTOR) * (PULSE_MIN + (1.0F - PULSE_MIN) * wave);

        //? if <26.1 {
        Vec3 camPos = camera.getPosition();
        //?} else if <26.2 {
        /*Vec3 camPos = cameraState.pos;
        SubmitNodeStorage storage = mc.gameRenderer.getSubmitNodeStorage();
        PoseStack poseStack = new PoseStack();
        *///?} else {
        /*Vec3 camPos = cameraState.pos;
        SubmitNodeStorage storage = new SubmitNodeStorage();
        PoseStack poseStack = new PoseStack();
        *///?}

        //? if <26.1 {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        renderBlocks(mc, poseStack, camPos, bufferSource, r, g, b, pulsed);
        //?} else if <26.2 {
        /*RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), clearDepth);

        try {
            BlockModelRenderState blockRenderState = new BlockModelRenderState();
            BlockModelResolver resolver = new BlockModelResolver(mc.getModelManager());
            for (Map.Entry<BlockPos, Float> e : blockFade.entrySet()) {
                submitBlock(mc, e.getKey(), poseStack, camPos, storage, resolver, blockRenderState, r, g, b, pulsed * e.getValue());
            }
        *///?} else if <26.3 {
        /*RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), clearDepth);

        BlockModelRenderState blockRenderState = new BlockModelRenderState();
        BlockModelResolver resolver = new BlockModelResolver(mc.getModelManager());
        for (Map.Entry<BlockPos, Float> e : blockFade.entrySet()) {
            submitBlock(mc, e.getKey(), poseStack, camPos, storage, resolver, blockRenderState, r, g, b, pulsed * e.getValue());
        }
        *///?} else {
        /*BlockModelRenderState blockRenderState = new BlockModelRenderState();
        BlockModelResolver resolver = new BlockModelResolver(mc.getModelManager());
        for (Map.Entry<BlockPos, Float> e : blockFade.entrySet()) {
            submitBlock(mc, e.getKey(), poseStack, camPos, storage, resolver, blockRenderState, r, g, b, pulsed * e.getValue());
        }
        *///?}

        //? if <26.1 {
        for (Map.Entry<Entity, Float> e : entityFade.entrySet()) {
            Entity entity = e.getKey();
            if (entity.isRemoved()) {
                continue;
        //?} else if <26.2 {
            /*EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            for (Map.Entry<Entity, Float> e : entityFade.entrySet()) {
                Entity entity = e.getKey();
                if (entity.isRemoved()) {
                    continue;
                }
                EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
                double x = state.x - camPos.x;
                double y = state.y - camPos.y;
                double z = state.z - camPos.z;
                dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, 1f, 1f, 1f, 0f, FULL_FILL, true));
                dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, r, g, b, pulsed * e.getValue(), FULL_FILL, false));
        *///?} else {
        /*EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        for (Map.Entry<Entity, Float> e : entityFade.entrySet()) {
            Entity entity = e.getKey();
            if (entity.isRemoved()) {
                continue;
        *///?}
            }
    //? if <26.1 {
            renderEntity(mc, poseStack, partialTick, camPos, bufferSource, entity, r, g, b, pulsed * e.getValue());
        }
    //?} else if <26.2 {
            /*mc.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            mc.renderBuffers().bufferSource().endBatch();
        } finally {
            storage.clear();
        }
    }

    private static void submitBlock(Minecraft mc, BlockPos pos, PoseStack poseStack, Vec3 camPos, SubmitNodeCollector collector,
                                    BlockModelResolver resolver, BlockModelRenderState blockRenderState, float r, float g, float b, float a) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        Vec3 offset = state.getOffset(pos);
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x + offset.x, pos.getY() - camPos.y + offset.y, pos.getZ() - camPos.z + offset.z);
        resolver.update(blockRenderState, state, BlockDisplayContext.create());
        for (int i = 0; i < blockRenderState.tintLayers().size(); i++) {
            blockRenderState.tintLayers().set(i, -1);
        }
        ScanNodeCollector.cullFaces = ScanOverlayRenderer.getHiddenFaces(mc, pos, blockFade.keySet());
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, 1f, 1f, 1f, 0f, FULL_FILL, true), 15728880, OverlayTexture.NO_OVERLAY, 0);
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, r, g, b, a, FULL_FILL, false), 15728880, OverlayTexture.NO_OVERLAY, 0);
        ScanNodeCollector.cullFaces = Set.of();
        poseStack.popPose();
    *///?} else if <26.3 {
            /*EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
            double x = state.x - camPos.x;
            double y = state.y - camPos.y;
            double z = state.z - camPos.z;
            dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, 1f, 1f, 1f, 0f, FULL_FILL, true));
            dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, r, g, b, pulsed * e.getValue(), FULL_FILL, false));
        }

        mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
    }

    private static void submitBlock(Minecraft mc, BlockPos pos, PoseStack poseStack, Vec3 camPos, SubmitNodeCollector collector,
                                    BlockModelResolver resolver, BlockModelRenderState blockRenderState, float r, float g, float b, float a) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        Vec3 offset = state.getOffset(pos);
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x + offset.x, pos.getY() - camPos.y + offset.y, pos.getZ() - camPos.z + offset.z);
        resolver.update(blockRenderState, state, BlockDisplayContext.create());
        for (int i = 0; i < blockRenderState.tintLayers().size(); i++) {
            blockRenderState.tintLayers().set(i, -1);
        }
        ScanNodeCollector.cullFaces = ScanOverlayRenderer.getHiddenFaces(mc, pos, blockFade.keySet());
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, 1f, 1f, 1f, 0f, FULL_FILL, true), 15728880, OverlayTexture.NO_OVERLAY, 0);
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, r, g, b, a, FULL_FILL, false), 15728880, OverlayTexture.NO_OVERLAY, 0);
        ScanNodeCollector.cullFaces = Set.of();
        poseStack.popPose();
    *///?} else {
            /*EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
            double x = state.x - camPos.x;
            double y = state.y - camPos.y;
            double z = state.z - camPos.z;
            dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, 1f, 1f, 1f, 0f, FULL_FILL, true));
            dispatcher.submit(state, cameraState, x, y, z, poseStack, new ScanNodeCollector(storage, r, g, b, pulsed * e.getValue(), FULL_FILL, false));
        }

        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        try (FeatureRenderDispatcher.PreparedFrame frame = mc.gameRenderer.featureRenderDispatcher().prepareFrame(storage);
             RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "FieldGuide discovery overlay", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.of(clearDepth))) {
            RenderSystem.bindDefaultUniforms(renderPass);
            FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
        }
    }

    private static void submitBlock(Minecraft mc, BlockPos pos, PoseStack poseStack, Vec3 camPos, SubmitNodeCollector collector,
                                    BlockModelResolver resolver, BlockModelRenderState blockRenderState, float r, float g, float b, float a) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        Vec3 offset = state.getOffset(pos);
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x + offset.x, pos.getY() - camPos.y + offset.y, pos.getZ() - camPos.z + offset.z);
        resolver.update(blockRenderState, state, BlockDisplayContext.create());
        for (int i = 0; i < blockRenderState.tintLayers().size(); i++) {
            blockRenderState.tintLayers().set(i, -1);
        }
        ScanNodeCollector.cullFaces = ScanOverlayRenderer.getHiddenFaces(mc, pos, blockFade.keySet());
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, 1f, 1f, 1f, 0f, FULL_FILL, true), 15728880, OverlayTexture.NO_OVERLAY, 0);
        blockRenderState.submit(poseStack, new ScanNodeCollector(collector, r, g, b, a, FULL_FILL, false), 15728880, OverlayTexture.NO_OVERLAY, 0);
        ScanNodeCollector.cullFaces = Set.of();
        poseStack.popPose();
    *///?}
    }

    private static void advanceFade(Map<BlockPos, Float> map, Set<BlockPos> current, float step) {
        Iterator<Map.Entry<BlockPos, Float>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Float> e = it.next();
            if (current.contains(e.getKey())) {
                e.setValue(Math.min(1.0F, e.getValue() + step));
            } else {
                float v = e.getValue() - step;
                if (v <= 0.0F) {
                    it.remove();
                } else {
                    e.setValue(v);
                }
            }
        }
        for (BlockPos pos : current) {
            map.putIfAbsent(pos, step);
        }
    }

    private static void advanceEntityFade(Map<Entity, Float> map, Set<Entity> current, float step) {
        Iterator<Map.Entry<Entity, Float>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, Float> e = it.next();
            if (e.getKey().isRemoved()) {
                it.remove();
                continue;
            }
            if (current.contains(e.getKey())) {
                e.setValue(Math.min(1.0F, e.getValue() + step));
            } else {
                float v = e.getValue() - step;
                if (v <= 0.0F) {
                    it.remove();
                } else {
                    e.setValue(v);
                }
            }
        }
        for (Entity entity : current) {
            map.putIfAbsent(entity, step);
        }
    }

    private static List<Entity> gatherEntityTargets(Minecraft mc, Player player, double searchRange, Entity activeEntity, Frustum frustum) {
        List<Entity> result = new ArrayList<>();
        AABB box = player.getBoundingBox().inflate(searchRange);
        double maxSq = searchRange * searchRange;
        Vec3 eye = player.getEyePosition(1.0F);
        for (Entity raw : mc.level.getEntities(player, box, e -> true)) {
            Entity entity = ScanOverlayRenderer.resolveEntity(raw);
            if (entity == null || entity == player || entity == activeEntity) {
                continue;
            }
            if (entity.distanceToSqr(eye.x, eye.y, eye.z) > maxSq) {
                continue;
            }
            if (!frustum.isVisible(entity.getBoundingBox())) {
                continue;
            }
            if (!isScannableEntity(entity)) {
                continue;
            }
            if (!isEntityPartiallyVisible(mc.level, player, eye, entity)) {
                continue;
            }
            result.add(entity);
        }
        return result;
    }

    private static void refreshBlockTargets(Minecraft mc, Player player, double searchRange, Frustum frustum) {
        Level level = mc.level;
        if (level != cachedLevel) {
            cachedLevel = level;
            blockTargets.clear();
            lastRecomputeTime = Long.MIN_VALUE;
        }
        long now = level.getGameTime();
        if (now == lastRecomputeTime) {
            return;
        }
        if (lastRecomputeTime != Long.MIN_VALUE && now - lastRecomputeTime < RECOMPUTE_INTERVAL) {
            return;
        }
        lastRecomputeTime = now;
        blockTargets.clear();

        BlockPos center = player.blockPosition();
        int radius = (int) Math.ceil(searchRange);
        double maxSq = searchRange * searchRange;
        double px = player.getX();
        double py = player.getEyeY();
        double pz = player.getZ();
        //? if <26.1 {
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
        //?} else {
        /*int minY = Math.max(level.getMinY(), center.getY() - radius);
        int maxY = Math.min(level.getMaxY(), center.getY() + radius);
        *///?}
        int chunkRadius = (radius >> 4) + 1;
        int pcx = center.getX() >> 4;
        int pcz = center.getZ() >> 4;

        Map<Block, Integer> typeCode = new IdentityHashMap<>();
        List<BlockPos> found = new ArrayList<>();

        List<long[]> chunks = new ArrayList<>();
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                chunks.add(new long[]{cx, cz});
            }
        }
        chunks.sort(Comparator.comparingDouble(c -> {
            double ccx = (c[0] << 4) + 8 - px;
            double ccz = (c[1] << 4) + 8 - pz;
            return ccx * ccx + ccz * ccz;
        }));

        outer:
        for (long[] c : chunks) {
            int cx = (int) c[0];
            int cz = (int) c[1];
            LevelChunk chunk = level.getChunk(cx, cz);
            LevelChunkSection[] sections = chunk.getSections();
            //? if <26.1 {
            int minSection = chunk.getMinSection();
            //?} else {
            /*int minSection = chunk.getMinSectionY();
            *///?}
            for (int si = 0; si < sections.length; si++) {
                LevelChunkSection section = sections[si];
                if (section == null || section.hasOnlyAir()) {
                    continue;
                }
                int baseY = (minSection + si) << 4;
                if (baseY + 15 < minY || baseY > maxY) {
                    continue;
                }
                for (int ly = 0; ly < 16; ly++) {
                    int wy = baseY + ly;
                    if (wy < minY || wy > maxY) {
                        continue;
                    }
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            BlockState state = section.getBlockState(lx, ly, lz);
                            if (state.isAir()) {
                                continue;
                            }
                            int wx = (cx << 4) + lx;
                            int wz = (cz << 4) + lz;
                            double dx = wx + 0.5 - px;
                            double dy = wy + 0.5 - py;
                            double dz = wz + 0.5 - pz;
                            if (dx * dx + dy * dy + dz * dz > maxSq) {
                                continue;
                            }
                            Block block = state.getBlock();
                            Integer code = typeCode.get(block);
                            if (code == null) {
                                code = classifyBlock(block);
                                typeCode.put(block, code);
                            }
                            if (code == 0) {
                                continue;
                            }
                            if (!frustum.isVisible(new AABB(wx, wy, wz, wx + 1, wy + 1, wz + 1))) {
                                continue;
                            }
                            found.add(new BlockPos(wx, wy, wz));
                            if (found.size() >= MAX_FOUND) {
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        found.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(px, py, pz)));
        Vec3 eye = player.getEyePosition(1.0F);
        for (BlockPos pos : found) {
            if (blockTargets.size() >= MAX_RENDERED) {
                break;
            }
            if (!isBlockPartiallyVisible(level, player, eye, pos)) {
                continue;
            }
            Integer code = typeCode.get(level.getBlockState(pos).getBlock());
            if (code != null && (code == 1 || (code == 2 && isCompositeScannableAt(mc, pos)))) {
                blockTargets.add(pos);
            }
        }
    }

    private static boolean isBlockPartiallyVisible(Level level, Player player, Vec3 eye, BlockPos pos) {
        for (double[] o : SAMPLE_OFFSETS) {
            Vec3 point = new Vec3(pos.getX() + o[0], pos.getY() + o[1], pos.getZ() + o[2]);
            if (sightClear(level, player, eye, point, pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEntityPartiallyVisible(Level level, Player player, Vec3 eye, Entity entity) {
        AABB box = entity.getBoundingBox();
        Vec3[] points = {
                box.getCenter(),
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        for (Vec3 point : points) {
            if (sightClear(level, player, eye, point, null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sightClear(Level level, Player player, Vec3 from, Vec3 to, BlockPos targetPos) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return hit.getBlockPos().equals(targetPos);
    }

    private static int classifyBlock(Block block) {
        try {
            ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
            Object entry = manager.getEntryForTarget(block);
            if (entry == null || manager.getCategoryForEntry(entry) == null) {
                return 0;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!FieldGuideScanManager.needsVariantScan(entry, id)) {
                return 0;
            }
            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
            if (entryId != null && !ProgressManager.getInstance().canScanToUnlock(entryId)) {
                return 0;
            }
            return entry instanceof GuideEntry ge && ge.isComposite() ? 2 : 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean isCompositeScannableAt(Minecraft mc, BlockPos pos) {
        try {
            Block block = mc.level.getBlockState(pos).getBlock();
            Object entry = FieldGuideRaytracer.getInstance().getContextAwareEntry(block, mc, pos);
            if (entry == null) {
                return false;
            }
            ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
            if (manager.getCategoryForEntry(entry) == null) {
                return false;
            }
            ResourceLocation rawId = BuiltInRegistries.BLOCK.getKey(block);
            if (!FieldGuideScanManager.needsVariantScan(entry, rawId, mc.level, pos)) {
                return false;
            }
            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
            return entryId == null || ProgressManager.getInstance().canScanToUnlock(entryId);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isScannableEntity(Entity entity) {
        try {
            Minecraft mc = Minecraft.getInstance();
            EntityType<?> type = entity.getType();
            Object key = entity instanceof ItemEntity itemEntity ? itemEntity.getItem().getItem() : type;
            ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
            Object entry = FieldGuideRaytracer.getInstance().getContextAwareEntry(key, mc, entity.blockPosition());
            if (entry == null || manager.getCategoryForEntry(entry) == null) {
                return false;
            }
            ResourceLocation id = entity instanceof ItemEntity itemEntity
                    ? BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem())
                    : BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!FieldGuideScanManager.needsVariantScan(entry, id, mc.level, entity.blockPosition())) {
                return false;
            }
            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
            return entryId == null || ProgressManager.getInstance().canScanToUnlock(entryId);
        } catch (Exception e) {
            return false;
        }
    }

    //? if <26.1 {
    private static void renderBlocks(Minecraft mc, PoseStack poseStack, Vec3 camPos, MultiBufferSource.BufferSource bufferSource,
                                     float r, float g, float b, float pulsed) {
        setupBlockShaderUniforms(1.0F, 1.0F, 1.0F);
        for (Map.Entry<BlockPos, Float> e : blockFade.entrySet()) {
            emitBlock(mc, e.getKey(), poseStack, camPos, bufferSource, true, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        bufferSource.endBatch();

        setupBlockShaderUniforms(r, g, b);
        for (Map.Entry<BlockPos, Float> e : blockFade.entrySet()) {
            emitBlock(mc, e.getKey(), poseStack, camPos, bufferSource, false, r, g, b, pulsed * e.getValue());
        }
        bufferSource.endBatch();
    }

    private static void setupBlockShaderUniforms(float r, float g, float b) {
        if (ModRenderTypes.SCAN_BLOCK_SHADER != null) {
            ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ScanLimitY").set(FULL_FILL);
            ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("InverseModelViewMat").set(new Matrix4f());
            if (ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator") != null) {
                ModRenderTypes.SCAN_BLOCK_SHADER.getUniform("ColorModulator").set(r, g, b, 1.0F);
            }
        }
    }

    private static void emitBlock(Minecraft mc, BlockPos pos, PoseStack poseStack, Vec3 camPos, MultiBufferSource.BufferSource bufferSource, boolean depth, float r, float g, float b, float a) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        Vec3 offset = state.getOffset(mc.level, pos);
        double x = pos.getX() - camPos.x + offset.x;
        double y = pos.getY() - camPos.y + offset.y;
        double z = pos.getZ() - camPos.z + offset.z;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        drawBlock(mc, state, pos, poseStack, bufferSource, depth, r, g, b, a);
        poseStack.popPose();
    }

    private static void drawBlock(Minecraft mc, BlockState state, BlockPos pos, PoseStack poseStack,
                                  MultiBufferSource.BufferSource bufferSource, boolean depth, float r, float g, float b, float a) {
        MultiBufferSource source = new ScanOverlayRenderer.ScanBufferSourceWrapper(bufferSource, r, g, b, a, depth, false);

        if (state.getRenderShape() == RenderShape.MODEL) {
            RenderType type = ItemBlockRenderTypes.getRenderType(state, false);
            ScanOverlayRenderer.renderBlockModelAsShell(mc, state, pos, poseStack, source.getBuffer(type), blockFade.keySet());
        } else {
            mc.getBlockRenderer().renderSingleBlock(state, poseStack, source, 15728880, OverlayTexture.pack(0, 10));
        }
    }

    private static void renderEntity(Minecraft mc, PoseStack poseStack, float partialTick, Vec3 camPos,
                                     MultiBufferSource.BufferSource bufferSource, Entity entity, float r, float g, float b, float a) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camPos.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camPos.y;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camPos.z;
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        if (ModRenderTypes.SCAN_ENTITY_SHADER != null) {
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ScanLimitY").set(FULL_FILL);
            Matrix4f modelView = new Matrix4f(poseStack.last().pose());
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("InverseModelViewMat").set(modelView.invert());
            if (ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
                ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

        MultiBufferSource depthSource = new ScanOverlayRenderer.ScanBufferSourceWrapper(bufferSource, 1.0F, 1.0F, 1.0F, 1.0F, true, true);
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, depthSource, 15728880);
        bufferSource.endBatch();

        if (ModRenderTypes.SCAN_ENTITY_SHADER != null && ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator") != null) {
            ModRenderTypes.SCAN_ENTITY_SHADER.getUniform("ColorModulator").set(r, g, b, 1.0F);
        }

        MultiBufferSource colorSource = new ScanOverlayRenderer.ScanBufferSourceWrapper(bufferSource, r, g, b, a, false, true);
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, colorSource, 15728880);
        bufferSource.endBatch();

        poseStack.popPose();
    }
    //?}
}

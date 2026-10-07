package com.evandev.fieldguide.client.render;

//? if >=26.1 {
/*import com.evandev.fieldguide.client.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//? if <26.2 {
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?}

//? if >=26.2 {
/^import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.shapes.VoxelShape;
^///?}

//? if >=26.3 {
/^import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
^///?}

//? if <26.2 {
public record ScanNodeCollector(SubmitNodeCollector delegate, float r, float g, float b, float a, float limitY,
//?} else {
/^public record ScanNodeCollector(OrderedSubmitNodeCollector delegate, float r, float g, float b, float a, float limitY,
^///?}
                                boolean isDepth) implements SubmitNodeCollector {

    public static Set<Direction> cullFaces = Set.of();

    public static List<BlockStateModelPart> cull(List<BlockStateModelPart> parts) {
        if (cullFaces.isEmpty()) return parts;
        Set<Direction> hidden = Set.copyOf(cullFaces);
        List<BlockStateModelPart> culled = new ArrayList<>(parts.size());
        for (BlockStateModelPart part : parts) {
            culled.add(new CulledPart(part, hidden));
        }
        return culled;
    }

    private record CulledPart(BlockStateModelPart delegate, Set<Direction> hidden) implements BlockStateModelPart {
        @Override
        public @NonNull List<BakedQuad> getQuads(Direction direction) {
            return direction != null && hidden.contains(direction) ? List.of() : delegate.getQuads(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public Material.@NonNull Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }
    }

    private RenderType wrap(RenderType renderType) {
        return isDepth ? ModRenderTypes.wrapForDepth(renderType) : ModRenderTypes.wrapForScan(renderType);
    }

    private int scanLight() {
        return ScanOverlayRenderer.getPackedScanLightCoords(limitY, r, g, b, a);
    }

    @Override
    public @NonNull OrderedSubmitNodeCollector order(int order) {
        //? if <26.2 {
        return this;
        //?} else {
        /^return delegate instanceof SubmitNodeCollector collector
                ? new ScanNodeCollector(collector.order(order), r, g, b, a, limitY, isDepth)
                : this;
        ^///?}
    }

    @Override
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull List<BlockStateModelPart> parts, int @NonNull [] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitBlockModel(poseStack, wrapped, cull(parts), tintLayers, scanLight(), overlayCoords, outlineColor);
    }

    @Override
    //? if <26.2 {
    public <S> void submitModel(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitModel(model, state, poseStack, wrapped, scanLight(), overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitModelPart(modelPart, poseStack, wrapped, scanLight(), overlayCoords, sprite, sheeted, false, tintedColor, crumblingOverlay, outlineColor);
    //?} else if <26.3 {
    /^public <S> void submitModel(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitModel(model, state, poseStack, wrapped, scanLight(), overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, TextureAtlasSprite sprite, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitModelPart(modelPart, poseStack, wrapped, scanLight(), overlayCoords, sprite, tintedColor, crumblingOverlay, outlineColor);
    ^///?} else {
    /^public <S> void submitModel(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, UvMapping uvMapping, int outlineColor) {
        RenderType wrapped = wrap(renderType);
        if (wrapped == null) return;
        delegate.submitModel(model, state, poseStack, wrapped, scanLight(), overlayCoords, tintedColor, uvMapping, outlineColor);
    ^///?}
    }

    @Override
    //? if <26.3 {
    public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull List<BakedQuad> quads, ItemStackRenderState.@NonNull FoilType foilType) {
    //?} else {
    /^public <S> void submitCrumblingOverlay(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.@NonNull CrumblingOverlay crumblingOverlay) {
    }

    @Override
    public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull ItemQuads quads, ItemStackRenderState.@NonNull FoilType foilType) {
    ^///?}
        Map<RenderType, List<BakedQuad>> byType = new IdentityHashMap<>();
        //? if <26.3 {
        for (BakedQuad quad : quads) {
        //?} else {
        /^for (BakedQuad quad : quads.all()) {
        ^///?}
            RenderType wrapped = wrap(quad.materialInfo().itemRenderType());
            if (wrapped != null) {
                byType.computeIfAbsent(wrapped, k -> new ArrayList<>()).add(quad);
            }
        }

        int light = scanLight();
        for (Map.Entry<RenderType, List<BakedQuad>> group : byType.entrySet()) {
            List<BakedQuad> groupQuads = group.getValue();
            delegate.submitCustomGeometry(poseStack, group.getKey(), (pose, buffer) -> {
                QuadInstance instance = new QuadInstance();
                instance.setLightCoords(light);
                instance.setOverlayCoords(overlayCoords);
                instance.setColor(-1);
                for (BakedQuad quad : groupQuads) {
                    buffer.putBakedQuad(pose, quad, instance);
                }
            });
        }
    }

    @Override
    public void submitShadow(@NonNull PoseStack poseStack, float radius, @NonNull List<EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    //? if <26.2 {
    public void submitNameTag(@NonNull PoseStack poseStack, Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, @NonNull CameraRenderState camera) {
    //?} else if <26.3 {
    /^public void submitNameTag(@NonNull PoseStack poseStack, Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, @NonNull CameraRenderState camera) {
    ^///?} else {
    /^public void submitNameTag(@NonNull PoseStack poseStack, Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, @NonNull CameraRenderState camera) {
    }

    @Override
    public void submitTextBackground(@NonNull PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.@NonNull DisplayMode displayMode, int lightCoords) {
    ^///?}
    }

    @Override
    public void submitText(@NonNull PoseStack poseStack, float x, float y, net.minecraft.util.@NonNull FormattedCharSequence string, boolean dropShadow, net.minecraft.client.gui.Font.@NonNull DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
    }

    @Override
    public void submitFlame(@NonNull PoseStack poseStack, @NonNull EntityRenderState renderState, @NonNull Quaternionf rotation) {
    }

    @Override
    public void submitLeash(@NonNull PoseStack poseStack, EntityRenderState.@NonNull LeashState leashState) {
    }

    @Override
    //? if <26.2 {
    public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState) {
    //?} else {
    /^public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState, int outlineColor) {
    ^///?}
    }

    @Override
    //? if <26.2 {
    public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull BlockStateModel model, long seed, int progress) {
    //?} else if <26.3 {
    /^public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull List<BlockStateModelPart> parts, int progress) {
    }

    @Override
    public void submitShapeOutline(@NonNull PoseStack poseStack, @NonNull VoxelShape shape, @NonNull RenderType renderType, int color, float width, boolean afterTerrain) {
    ^///?} else {
    /^public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
    }

    @Override
    public void submitShapeOutline(@NonNull PoseStack poseStack, @NonNull VoxelShape shape, @NonNull RenderType renderType, int color, float width, boolean afterTerrain) {
    ^///?}
    }

    @Override
    public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
        RenderType wrapped = wrap(renderType);
        if (wrapped != null) {
            delegate.submitCustomGeometry(poseStack, wrapped, customGeometryRenderer);
        }
    }

    @Override
    //? if <26.2 {
    public void submitParticleGroup(@NonNull ParticleGroupRenderer particleGroupRenderer) {
    //?} else {
    /^public void submitQuadParticleGroup(@NonNull QuadParticleRenderState particles) {
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.@NonNull Group group, @NonNull CameraRenderState camera, boolean onTop) {
    ^///?}
    }
}
*///?}

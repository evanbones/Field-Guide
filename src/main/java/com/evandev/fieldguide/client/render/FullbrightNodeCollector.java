package com.evandev.fieldguide.client.render;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

import java.util.List;

//? if <26.2 {
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?}

//? if >=26.2 {
/^import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.shapes.VoxelShape;
^///?}

//? if >=26.3 {
/^import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
^///?}

//? if <26.2 {
public record FullbrightNodeCollector(SubmitNodeCollector delegate) implements SubmitNodeCollector {
//?} else {
/^public record FullbrightNodeCollector(OrderedSubmitNodeCollector delegate) implements SubmitNodeCollector {
^///?}
    @Override
    public @NonNull OrderedSubmitNodeCollector order(int order) {
        //? if <26.2 {
        return this;
        //?} else {
        /^return delegate instanceof SubmitNodeCollector collector ? new FullbrightNodeCollector(collector.order(order)) : this;
        ^///?}
    }

    @Override
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull List<BlockStateModelPart> parts, int @NonNull [] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        delegate.submitBlockModel(poseStack, renderType, parts, tintLayers, 15728880, overlayCoords, outlineColor);
    }

    @Override
    //? if <26.3 {
    public <S> void submitModel(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        delegate.submitModel(model, state, poseStack, renderType, 15728880, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    //?} else {
    /^public <S> void submitModel(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, UvMapping uvMapping, int outlineColor) {
        delegate.submitModel(model, state, poseStack, renderType, 15728880, overlayCoords, tintedColor, uvMapping, outlineColor);
    ^///?}
    }

    @Override
    //? if <26.2 {
    public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
        delegate.submitModelPart(modelPart, poseStack, renderType, 15728880, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
    //?} else if <26.3 {
    /^public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, TextureAtlasSprite sprite, int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int outlineColor) {
        delegate.submitModelPart(modelPart, poseStack, renderType, 15728880, overlayCoords, sprite, tintedColor, crumblingOverlay, outlineColor);
    ^///?} else {
    /^public <S> void submitCrumblingOverlay(@NonNull Model<? super S> model, S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, ModelFeatureRenderer.@NonNull CrumblingOverlay crumblingOverlay) {
        delegate.submitCrumblingOverlay(model, state, poseStack, renderType, 15728880, overlayCoords, tintedColor, crumblingOverlay);
    ^///?}
    }

    @Override
    public void submitShadow(@NonNull PoseStack poseStack, float radius, @NonNull List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override
    //? if <26.2 {
    public void submitNameTag(@NonNull PoseStack poseStack, Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, @NonNull CameraRenderState camera) {
        delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, 15728880, distanceToCameraSq, camera);
    //?} else {
    /^public void submitNameTag(@NonNull PoseStack poseStack, Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, @NonNull CameraRenderState camera) {
        delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, 15728880, camera);
    ^///?}
    }

    @Override
    public void submitText(@NonNull PoseStack poseStack, float x, float y, @NonNull FormattedCharSequence string, boolean dropShadow, Font.@NonNull DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, 15728880, color, backgroundColor, outlineColor);
    //? if >=26.3 {
    /^}

    @Override
    public void submitTextBackground(@NonNull PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.@NonNull DisplayMode displayMode, int lightCoords) {
        delegate.submitTextBackground(poseStack, x0, y0, x1, y1, color, displayMode, 15728880);
    ^///?}
    }

    @Override
    public void submitFlame(@NonNull PoseStack poseStack, @NonNull EntityRenderState renderState, @NonNull Quaternionf rotation) {
        delegate.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(@NonNull PoseStack poseStack, EntityRenderState.@NonNull LeashState leashState) {
        delegate.submitLeash(poseStack, leashState);
    }

    @Override
    //? if <26.2 {
    public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState) {
        delegate.submitMovingBlock(poseStack, movingBlockRenderState);
    //?} else {
    /^public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
    ^///?}
    }

    @Override
    //? if <26.2 {
    public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull BlockStateModel model, long seed, int progress) {
        delegate.submitBreakingBlockModel(poseStack, model, seed, progress);
    //?} else if <26.3 {
    /^public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull List<BlockStateModelPart> parts, int progress) {
        delegate.submitBreakingBlockModel(poseStack, parts, progress);
    }

    @Override
    public void submitShapeOutline(@NonNull PoseStack poseStack, @NonNull VoxelShape shape, @NonNull RenderType renderType, int color, float width, boolean afterTerrain) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    ^///?} else {
    /^public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
        delegate.submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent);
    }

    @Override
    public void submitShapeOutline(@NonNull PoseStack poseStack, @NonNull VoxelShape shape, @NonNull RenderType renderType, int color, float width, boolean afterTerrain) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    ^///?}
    }

    @Override
    public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
        delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    @Override
    //? if <26.2 {
    public void submitParticleGroup(@NonNull ParticleGroupRenderer particleGroupRenderer) {
        delegate.submitParticleGroup(particleGroupRenderer);
    //?} else {
    /^public void submitQuadParticleGroup(@NonNull QuadParticleRenderState particleGroupRenderer) {
        delegate.submitQuadParticleGroup(particleGroupRenderer);
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.@NonNull Group group, @NonNull CameraRenderState camera, boolean onTop) {
        delegate.submitGizmoPrimitives(group, camera, onTop);
    ^///?}
    }

    @Override
    //? if <26.3 {
    public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext itemDisplayContext, int i, int i1, int i2, int @NonNull [] ints, @NonNull List<BakedQuad> list, ItemStackRenderState.@NonNull FoilType foilType) {
    //?} else {
    /^public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext itemDisplayContext, int i, int i1, int i2, int @NonNull [] ints, @NonNull ItemQuads list, ItemStackRenderState.@NonNull FoilType foilType) {
    ^///?}
        delegate.submitItem(poseStack, itemDisplayContext, i, i1, i2, ints, list, foilType);
    }
}
*///?}

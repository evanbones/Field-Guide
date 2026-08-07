package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.client.ModRenderTypes;
import com.evandev.fieldguide.client.render.ScanNodeCollector;
import com.evandev.fieldguide.client.render.ScanOverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mixin(ScanNodeCollector.class)
public abstract class ScanNodeCollectorFabricMixin implements FabricOrderedSubmitNodeCollector {

    @Shadow
    @Final
    private OrderedSubmitNodeCollector delegate;

    @Shadow
    public abstract boolean isDepth();

    @Shadow
    public abstract float limitY();

    @Shadow
    public abstract float r();

    @Shadow
    public abstract float g();

    @Shadow
    public abstract float b();

    @Shadow
    public abstract float a();

    @Override
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull Function<ChunkSectionLayer, RenderType> renderTypeFunction, boolean translucent, List<BlockStateModelPart> parts, Mesh mesh, int @NonNull [] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {

        int packedLight = ScanOverlayRenderer.getPackedScanLightCoords(limitY(), r(), g(), b(), a());
        Map<RenderType, MutableMesh> meshMap = new HashMap<>();

        if (!parts.isEmpty()) {
            Direction[] directions = new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
            QuadEmitter temp = Renderer.get().quadEmitter(q -> {
            });
            for (BlockStateModelPart part : parts) {
                for (Direction dir : directions) {
                    for (BakedQuad quad : part.getQuads(dir)) {
                        temp.fromBakedQuad(quad);
                        RenderType original = renderTypeFunction.apply(temp.chunkLayer());
                        RenderType wrapped = isDepth() ? ModRenderTypes.wrapForDepth(original) : ModRenderTypes.wrapForScan(original);

                        MutableMesh targetMesh = meshMap.computeIfAbsent(wrapped, k -> Renderer.get().mutableMesh());
                        QuadEmitter emitter = targetMesh.emitter();
                        emitter.copyFrom(temp);
                        emitter.diffuseShade(false);
                        emitter.ambientOcclusion(TriState.FALSE);
                        emitter.emissive(false);
                        for (int i = 0; i < 4; i++) {
                            emitter.lightmap(i, packedLight);
                        }
                        emitter.emit();
                    }
                }
            }
        }

        if (mesh != null) {
            mesh.forEach(quad -> {
                RenderType original = renderTypeFunction.apply(quad.chunkLayer());
                RenderType wrapped = isDepth() ? ModRenderTypes.wrapForDepth(original) : ModRenderTypes.wrapForScan(original);

                MutableMesh targetMesh = meshMap.computeIfAbsent(wrapped, k -> Renderer.get().mutableMesh());
                QuadEmitter emitter = targetMesh.emitter();
                emitter.copyFrom(quad);
                emitter.diffuseShade(false);
                emitter.ambientOcclusion(TriState.FALSE);
                emitter.emissive(false);
                for (int i = 0; i < 4; i++) {
                    emitter.lightmap(i, packedLight);
                }
                emitter.emit();
            });
        }

        for (Map.Entry<RenderType, MutableMesh> entry : meshMap.entrySet()) {
            RenderType renderType = entry.getKey();
            Mesh finalMesh = entry.getValue().immutableCopy();

            if (finalMesh.size() > 0) {
                delegate.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                    QuadEmitter outEmitter = Renderer.get().quadEmitter(q -> {
                        q.buffer(overlayCoords, pose, buffer);
                    });
                    finalMesh.outputTo(outEmitter);
                });
            }
        }
    }

    @Inject(
            method = "submitBlockModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Ljava/util/List;[IIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSubmitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
        this.submitBlockModel(
                poseStack,
                layer -> renderType,
                renderType.hasBlending(),
                parts,
                null,
                tintLayers,
                lightCoords,
                overlayCoords,
                outlineColor
        );
        ci.cancel();
    }
}
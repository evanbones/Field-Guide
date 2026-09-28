package com.evandev.fieldguide.mixin.client;

//? if <26.1 {
import com.evandev.fieldguide.client.render.DiscoveryOverlayRenderer;
import com.evandev.fieldguide.client.render.ScanOverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "renderLevel",
            at = @At("RETURN")
    )
    private void renderScanOverlays(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        ScanOverlayRenderer.render(
                poseStack,
                partialTick,
                camera,
                Minecraft.getInstance().renderBuffers().bufferSource()
        );

        Frustum frustum = new Frustum(frustumMatrix, projectionMatrix);
        Vec3 camPos = camera.getPosition();
        frustum.prepare(camPos.x, camPos.y, camPos.z);

        DiscoveryOverlayRenderer.render(
                poseStack,
                partialTick,
                camera,
                frustum,
                Minecraft.getInstance().renderBuffers().bufferSource()
        );
    }
}
//?} else {
/*import com.evandev.fieldguide.client.render.AfterLevelOverlay;
import com.evandev.fieldguide.client.render.DiscoveryOverlayRenderer;
import com.evandev.fieldguide.client.render.ScanOverlayRenderer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <26.2 {
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//?}

//? if <26.3 {
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4fc;
//?}

//? if >=26.2 {
/^import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4fStack;
^///?}

//? if >=26.3 {
/^import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
^///?}

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            //? if <26.2 {
            method = "renderLevel",
            //?} else {
            /^method = "render",
            ^///?}
            //? if <26.2 {
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;", remap = false)
            //?} else {
            /^at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;close()V", shift = At.Shift.AFTER)
            ^///?}
    )
    //? if <26.2 {
    private void renderOverlays(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        fieldguide$renderOverlays(cameraState, 1.0);
    //?} else if <26.3 {
    /^private void renderOverlays(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        fieldguide$renderOverlays(cameraState, 0.0);
    ^///?} else {
    /^private void renderOverlays(GraphicsResourceAllocator resourceAllocator, boolean renderOutline, CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci) {
        fieldguide$renderOverlays(cameraState, 0.0);
    ^///?}
    }

    @Unique
    private static void fieldguide$renderOverlays(CameraRenderState cameraState, double clearDepth) {
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        //? if <26.2 {
        Camera camera = mc.gameRenderer.getMainCamera();
        //?} else {
        /^Camera camera = mc.gameRenderer.mainCamera();
        ^///?}

        //? if >=26.2 {
        /^Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(cameraState.viewRotationMatrix);
        try {
        ^///?}
        AfterLevelOverlay.render("FieldGuide scan overlay", clearDepth, collector -> ScanOverlayRenderer.render(new PoseStack(), partialTick, camera, collector));
        DiscoveryOverlayRenderer.render(partialTick, cameraState, clearDepth);
        //? if >=26.2 {
        /^} finally {
            modelViewStack.popMatrix();
        }
        ^///?}
    }
}
*///?}

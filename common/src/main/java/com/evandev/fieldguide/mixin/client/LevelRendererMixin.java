package com.evandev.fieldguide.mixin.client;

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
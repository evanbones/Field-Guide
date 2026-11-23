package com.evandev.fieldguide.client.render;

//? if <26.1 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class LevelOverlays {
    private static final Matrix4f POSE = new Matrix4f();
    private static final Matrix3f NORMAL = new Matrix3f();
    private static final Matrix4f FRUSTUM = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static Camera camera;
    private static float partialTick;
    private static boolean pending;
    private static boolean rendering;
    private static TextureTarget savedDepth;
    private static boolean depthSaved;

    public static void capture(PoseStack.Pose pose, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Camera cam, float tick) {
        POSE.set(pose.pose());
        NORMAL.set(pose.normal());
        FRUSTUM.set(frustumMatrix);
        PROJECTION.set(projectionMatrix);
        camera = cam;
        partialTick = tick;
        pending = true;
    }

    public static void renderPending() {
        if (!pending) return;
        pending = false;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(POSE);
        poseStack.last().normal().set(NORMAL);

        Frustum frustum = new Frustum(FRUSTUM, PROJECTION);
        Vec3 camPos = camera.getPosition();
        frustum.prepare(camPos.x, camPos.y, camPos.z);

        RenderSystem.depthMask(true);
        rendering = true;
        try {
            ScanOverlayRenderer.render(poseStack, partialTick, camera, mc.renderBuffers().bufferSource());
            DiscoveryOverlayRenderer.render(poseStack, partialTick, camera, frustum, mc.renderBuffers().bufferSource());
        } finally {
            rendering = false;
            camera = null;
            restoreDepth();
        }
    }

    public static void saveDepth() {
        if (depthSaved) return;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        if (savedDepth == null) {
            savedDepth = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        } else if (savedDepth.width != main.width || savedDepth.height != main.height) {
            savedDepth.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        savedDepth.copyDepthFrom(main);
        main.bindWrite(false);
        depthSaved = true;
    }

    private static void restoreDepth() {
        if (!depthSaved) return;
        depthSaved = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.copyDepthFrom(savedDepth);
        main.bindWrite(false);
    }

    public static boolean isRendering() {
        return rendering;
    }
}
//?}

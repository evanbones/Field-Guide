package com.evandev.fieldguide.client.render;

//? if >=26.1 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;

import java.util.function.Consumer;

//? if >=26.3 {
/^import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

import java.util.Optional;
import java.util.OptionalDouble;
^///?}

// Draws on top of the finished level with a cleared depth buffer
public final class AfterLevelOverlay {

    public static void render(String name, double clearDepth, Consumer<SubmitNodeCollector> submitter) {
        Minecraft mc = Minecraft.getInstance();
        //? if <26.2 {
        SubmitNodeStorage storage = mc.gameRenderer.getSubmitNodeStorage();
        try {
            submitter.accept(storage);
            if (storage.getSubmitsPerOrder().isEmpty()) return;
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), clearDepth);
            mc.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            mc.renderBuffers().bufferSource().endBatch();
        } finally {
            storage.clear();
        }
        //?} else if <26.3 {
        /^SubmitNodeStorage storage = new SubmitNodeStorage();
        submitter.accept(storage);
        if (storage.getSubmitsPerOrder().isEmpty()) return;
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), clearDepth);
        mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
        ^///?} else {
        /^SubmitNodeStorage storage = new SubmitNodeStorage();
        submitter.accept(storage);
        if (storage.getSubmitsPerOrder().isEmpty()) return;
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        try (FeatureRenderDispatcher.PreparedFrame frame = mc.gameRenderer.featureRenderDispatcher().prepareFrame(storage);
             RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> name, target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.of(clearDepth))) {
            RenderSystem.bindDefaultUniforms(renderPass);
            FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
        }
        ^///?}
    }
}
*///?}

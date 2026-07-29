package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderScanningIcon(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (!Services.PLATFORM.getPlatformName().equals("NeoForge")) {
            int xMouse = (int) this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
            int yMouse = (int) this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
            GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(this.minecraft, this.guiRenderState, xMouse, yMouse);
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            FieldGuideClient.renderScanningIcon(graphics, partialTick);
        }
    }
}
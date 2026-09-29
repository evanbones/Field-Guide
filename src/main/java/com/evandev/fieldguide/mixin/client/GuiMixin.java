package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.client.FieldGuideClient;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
*///?}

@Mixin(Gui.class)
//? if <1.21 {
/*public class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void renderScanningIcon(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
*///?} else if <26.1 {
public class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void renderScanningIcon(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
//?} else if <26.2 {
/*public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderScanningIcon(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
*///?} else {
/*public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderScanningIcon(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
*///?}
        //? if >=26.2 {
        /*int xMouse = (int) this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow());
        int yMouse = (int) this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow());
        GuiGraphics graphics = new GuiGraphics(this.minecraft, this.guiRenderState, xMouse, yMouse);
        *///?}
        //? if >=1.21 {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        //?}
        //? if <26.2 {
        FieldGuideClient.renderScanningIcon(guiGraphics, partialTick);
        //?} else {
        /*FieldGuideClient.renderScanningIcon(graphics, partialTick);
        *///?}
    }
}

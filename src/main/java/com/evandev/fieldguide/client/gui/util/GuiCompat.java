package com.evandev.fieldguide.client.gui.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.Minecraft;

//? if <26.1 {
import net.minecraft.client.gui.screens.Screen;
//?} else {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.renderer.RenderPipelines;
*///?}

//? if <1.21 {
/*import com.evandev.fieldguide.client.gui.util.legacy.LegacySprites;
*///?}

public final class GuiCompat {
    private GuiCompat() {
    }

    public static void push(GuiGraphics g) {
        //? if <26.1 {
        g.pose().pushPose();
        //?} else {
        /*g.pose().pushMatrix();
        *///?}
    }

    public static void pop(GuiGraphics g) {
        //? if <26.1 {
        g.pose().popPose();
        //?} else {
        /*g.pose().popMatrix();
        *///?}
    }

    public static void translate(GuiGraphics g, double x, double y) {
        translate(g, x, y, 0);
    }

    public static void translate(GuiGraphics g, double x, double y, double z) {
        //? if <26.1 {
        g.pose().translate(x, y, z);
        //?} else {
        /*g.pose().translate((float) x, (float) y);
        *///?}
    }

    public static void scale(GuiGraphics g, float scale) {
        //? if <26.1 {
        g.pose().scale(scale, scale, scale);
        //?} else {
        /*g.pose().scale(scale, scale);
        *///?}
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        //? if <26.1 {
        g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?}
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        //? if <26.1 {
        g.blit(texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight);
        //?} else {
        /*g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight);
        *///?}
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        //? if <1.21 {
        /*LegacySprites.blit(g, sprite, x, y, width, height);
        *///?} else if <26.1 {
        g.blitSprite(sprite, x, y, width, height);
        //?} else {
        /*g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        *///?}
    }

    public static float frameDeltaTicks() {
        //? if <1.21 {
        /*return Minecraft.getInstance().getDeltaFrameTime();
        *///?} else if <26.1 {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
        //?} else {
        /*return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        *///?}
    }

    public static void textWithWordWrap(GuiGraphics g, Font font, FormattedText text, int x, int y, int width, int color) {
        //? if <26.1 {
        g.drawWordWrap(font, text, x, y, width, color);
        //?} else {
        /*g.textWithWordWrap(font, text, x, y, width, color, false);
        *///?}
    }

    public static boolean hasShiftDown() {
        //? if <26.1 {
        return Screen.hasShiftDown();
        //?} else {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?}
    }

    public static boolean hasControlDown() {
        //? if <26.1 {
        return Screen.hasControlDown();
        //?} else {
        /*return Minecraft.getInstance().hasControlDown();
        *///?}
    }

    public static boolean isSelectAll(int keyCode) {
        //? if <26.1 {
        return Screen.isSelectAll(keyCode);
        //?} else {
        /*return keyCode == InputConstants.KEY_A && hasControlDown();
        *///?}
    }

    public static boolean isCopy(int keyCode) {
        //? if <26.1 {
        return Screen.isCopy(keyCode);
        //?} else {
        /*return keyCode == InputConstants.KEY_C && hasControlDown();
        *///?}
    }

    public static boolean isCut(int keyCode) {
        //? if <26.1 {
        return Screen.isCut(keyCode);
        //?} else {
        /*return keyCode == InputConstants.KEY_X && hasControlDown();
        *///?}
    }

    public static boolean isPaste(int keyCode) {
        //? if <26.1 {
        return Screen.isPaste(keyCode);
        //?} else {
        /*return keyCode == InputConstants.KEY_V && hasControlDown();
        *///?}
    }
}

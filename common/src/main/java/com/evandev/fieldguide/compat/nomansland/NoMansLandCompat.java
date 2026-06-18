package com.evandev.fieldguide.compat.nomansland;

import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class NoMansLandCompat {
    public static boolean isAvailable() {
        return Services.PLATFORM.isModLoaded("nomansland");
    }

    public static boolean isIntegrationUnlocked() {
        return isAvailable() && NoMansLandCompatImpl.isIntegrationUnlocked();
    }

    public static List<ResourceLocation> getDialoguesForEntry(Object coreEntry) {
        return isAvailable() ? NoMansLandCompatImpl.getDialoguesForEntry(coreEntry) : List.of();
    }

    public static List<ResourceLocation> getHeardDialogues(List<ResourceLocation> dialogues) {
        return isAvailable() ? NoMansLandCompatImpl.getHeardDialogues(dialogues) : List.of();
    }

    public static void startReplay(ResourceLocation dialogueLocation, String currentDescription) {
        if (isAvailable()) NoMansLandCompatImpl.startReplay(dialogueLocation, currentDescription);
    }

    public static void stopReplay() {
        if (isAvailable()) NoMansLandCompatImpl.stopReplay();
    }

    public static boolean isReplayActive() {
        return isAvailable() && NoMansLandCompatImpl.isReplayActive();
    }

    public static boolean isReplayTyping() {
        return isAvailable() && NoMansLandCompatImpl.isReplayTyping();
    }

    public static void renderDescriptionRewrite(GuiGraphics guiGraphics, Font font, int x, int y, int width, int normalColor) {
        if (isAvailable()) NoMansLandCompatImpl.renderDescriptionRewrite(guiGraphics, font, x, y, width, normalColor);
    }
}

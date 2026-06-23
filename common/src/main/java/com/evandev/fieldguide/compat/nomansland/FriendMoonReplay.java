package com.evandev.fieldguide.compat.nomansland;

import com.farcr.nomansland.common.friend.dialogue.DialogueState;
import com.farcr.nomansland.common.friend.dialogue.DialogueUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.List;

public final class FriendMoonReplay {
    private FriendMoonReplay() {}

    public static final int FRIEND_MOON_GREEN = 0xFF000000 | DialogueUtil.FRIEND_MOON_TEXT_COLOR;
    private static final ResourceLocation SPEAK_SOUND = ResourceLocation.fromNamespaceAndPath("nomansland", "entity.friend_moon.speak_bgr");
    public static final float INTRO_TICKS = 16f;
    public static final float PLACED_FADE_TICKS = 20f;
    public static final float HOLD_TICKS = 200f;
    public static final float FADE_BACK_TICKS = 20f;
    public static final float PULSE_AMPLITUDE = 0.5f;
    public static final float PULSE_SPEED = 0.12f;
    public static final int SPEAK_INTERVAL_TICKS = 2;
    private static final int LINE_HEIGHT = 9;

    private static boolean active = false;
    private static DialogueState state;
    private static String oldText = "";
    private static String newFull = "";
    private static int newVisibleTotal = 1;
    private static float[] revealTick = new float[0];
    private static int revealedCount = 0;
    private static float elapsedTicks = 0f;
    private static float lastSpeakTick = -1000f;
    private static float doneTick = -1f;

    public static void start(DialogueState dialogueState, String currentDescription) {
        state = dialogueState;
        oldText = currentDescription == null ? "" : currentDescription;

        List<String> fullLines = state.translateDialogue.constructText(Integer.MAX_VALUE);
        state.translateDialogue.flagDoneConstructed = false;
        newFull = String.join("\n", fullLines);
        int visible = 0;
        for (String line : fullLines) visible += line.length();
        newVisibleTotal = Math.max(visible, 1);

        revealTick = new float[newVisibleTotal + 1];
        revealedCount = 0;
        elapsedTicks = 0f;
        lastSpeakTick = -1000f;
        doneTick = -1f;
        active = true;
    }

    public static void reset() {
        active = false;
        state = null;
        oldText = "";
        newFull = "";
        revealTick = new float[0];
        revealedCount = 0;
        doneTick = -1f;
    }

    public static boolean isActive() {
        return active && state != null;
    }

    public static boolean isTyping() {
        return isActive() && elapsedTicks >= INTRO_TICKS && !state.doneTalking;
    }

    public static void render(GuiGraphics guiGraphics, Font font, int x, int y, int width, int normalColor) {
        if (!isActive()) return;
        Minecraft minecraft = Minecraft.getInstance();
        float delta = minecraft.isPaused() ? 0f : minecraft.getTimer().getGameTimeDeltaTicks();
        elapsedTicks += delta;
        int opaqueNormal = normalColor | 0xFF000000;

        RenderSystem.enableBlend();
        if (elapsedTicks < INTRO_TICKS) {
            float alpha = Mth.clamp(1f - (elapsedTicks / INTRO_TICKS), 0f, 1f);
            drawText(guiGraphics, font, x, y, width, oldText, 0, Integer.MAX_VALUE, FRIEND_MOON_GREEN, false, alpha);
            RenderSystem.disableBlend();
            return;
        }

        List<String> revealedLines = state.progressText(delta);
        int visible = 0;
        for (String line : revealedLines) visible += line.length();
        for (int k = revealedCount; k < visible && k < revealTick.length; k++) revealTick[k] = elapsedTicks;
        revealedCount = visible;

        if (!state.doneTalking) {
            drawText(guiGraphics, font, x, y, width, newFull, 0, visible, opaqueNormal, true, 1f);
            RenderSystem.disableBlend();
            tickSound();
            return;
        }

        if (doneTick < 0f) doneTick = elapsedTicks;
        float sinceDone = elapsedTicks - doneTick;
        if (sinceDone < HOLD_TICKS) {
            drawText(guiGraphics, font, x, y, width, newFull, 0, Integer.MAX_VALUE, opaqueNormal, true, 1f);
            RenderSystem.disableBlend();
            return;
        }
        float back = Mth.clamp((sinceDone - HOLD_TICKS) / FADE_BACK_TICKS, 0f, 1f);
        drawText(guiGraphics, font, x, y, width, oldText, 0, Integer.MAX_VALUE, opaqueNormal, false, back);
        drawText(guiGraphics, font, x, y, width, newFull, 0, Integer.MAX_VALUE, opaqueNormal, true, 1f - back);
        RenderSystem.disableBlend();
        if (back >= 1f) reset();
    }

    private static void drawText(GuiGraphics guiGraphics, Font font, int x, int y, int width,
                                 String full, int drawFrom, int drawTo, int baseColor, boolean typed, float alpha) {
        if (alpha <= 0.01f) return;
        int idx = 0, cx = x, cy = y, i = 0, n = full.length();
        while (i < n) {
            if (full.charAt(i) == '\n') {
                cx = x;
                cy += LINE_HEIGHT;
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < n && full.charAt(wordEnd) != ' ' && full.charAt(wordEnd) != '\n') wordEnd++;
            if (cx > x && cx + font.width(full.substring(i, wordEnd)) > x + width) {
                cx = x;
                cy += LINE_HEIGHT;
            }
            for (; i < wordEnd; i++) {
                if (idx >= drawFrom && idx < drawTo) {
                    int color = scaleAlpha(typed ? newColor(idx, baseColor) : baseColor, alpha);
                    guiGraphics.drawString(font, String.valueOf(full.charAt(i)), cx, cy, color, false);
                }
                cx += font.width(String.valueOf(full.charAt(i)));
                idx++;
            }
            if (i < n && full.charAt(i) == ' ') {
                cx += font.width(" ");
                idx++;
                i++;
            }
        }
    }

    private static int newColor(int idx, int normalColor) {
        float placement = revealTick.length == 0 ? 0f
            : 1f - Mth.clamp((elapsedTicks - revealTick[Math.min(idx, revealTick.length - 1)]) / PLACED_FADE_TICKS, 0f, 1f);
        float pulse = PULSE_AMPLITUDE * (0.5f + 0.5f * (float) Math.sin(elapsedTicks * PULSE_SPEED));
        float green = Math.max(placement, pulse);
        return lerpColor(normalColor, FRIEND_MOON_GREEN, green);
    }

    private static int scaleAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Mth.clamp(alpha, 0f, 1f));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int lerpColor(int from, int to, float t) {
        return FastColor.ARGB32.color(
            (int) Mth.lerp(t, FastColor.ARGB32.alpha(from), FastColor.ARGB32.alpha(to)),
            (int) Mth.lerp(t, FastColor.ARGB32.red(from), FastColor.ARGB32.red(to)),
            (int) Mth.lerp(t, FastColor.ARGB32.green(from), FastColor.ARGB32.green(to)),
            (int) Mth.lerp(t, FastColor.ARGB32.blue(from), FastColor.ARGB32.blue(to))
        );
    }

    private static void tickSound() {
        if (state.doneTalking) return;
        if (state.translateDialogue.isInPause((int) state.progress)) return;
        if (!state.canSpeakCurrently()) return;
        if (elapsedTicks - lastSpeakTick < SPEAK_INTERVAL_TICKS) return;
        lastSpeakTick = elapsedTicks;
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
            SPEAK_SOUND, SoundSource.VOICE, 1.0f, 1.0f,
            SoundInstance.createUnseededRandom(), false, 0,
            SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true
        ));
    }
}

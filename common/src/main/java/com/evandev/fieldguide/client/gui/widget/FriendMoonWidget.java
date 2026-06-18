package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.compat.nomansland.NoMansLandCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class FriendMoonWidget extends AbstractWidget {
    public static final ResourceLocation IDLE = texture("idle");
    public static final ResourceLocation HOVER = texture("hover");
    public static final ResourceLocation TALKING = texture("talking");
    public static final ResourceLocation UNHEARD = texture("unheard");
    private static final float TALK_FRAME_SPEED = 1f / 3f;

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/friend_moon/" + name + ".png");
    }

    private final List<ResourceLocation> dialogues;
    private final Supplier<String> descriptionSupplier;
    private float talkProgress = 0f;

    public FriendMoonWidget(int x, int y, int size, List<ResourceLocation> dialogues, Supplier<String> descriptionSupplier) {
        super(x, y, size, size, Component.translatable("gui.fieldguide.friend_moon"));
        this.dialogues = dialogues;
        this.descriptionSupplier = descriptionSupplier;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean heard = !NoMansLandCompat.getHeardDialogues(dialogues).isEmpty();
        this.active = heard;

        ResourceLocation texture;
        if (!heard) {
            talkProgress = 0f;
            texture = UNHEARD;
        } else if (NoMansLandCompat.isReplayTyping()) {
            float delta = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
            talkProgress = (talkProgress + (delta * TALK_FRAME_SPEED)) % 2f;
            texture = (((int) talkProgress) == 1) ? TALKING : HOVER;
        } else {
            talkProgress = 0f;
            texture = this.isHovered() ? HOVER : IDLE;
        }

        RenderSystem.enableBlend();
        guiGraphics.blit(texture, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        List<ResourceLocation> heard = NoMansLandCompat.getHeardDialogues(dialogues);
        if (heard.isEmpty() || Minecraft.getInstance().player == null) return;
        ResourceLocation pick = heard.get(Minecraft.getInstance().player.getRandom().nextInt(heard.size()));
        NoMansLandCompat.startReplay(pick, descriptionSupplier.get());
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (NoMansLandCompat.getHeardDialogues(dialogues).isEmpty()) return;
        handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.9F, 0.25F));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}

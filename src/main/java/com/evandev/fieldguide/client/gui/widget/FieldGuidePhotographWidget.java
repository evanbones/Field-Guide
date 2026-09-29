package com.evandev.fieldguide.client.gui.widget;

//? if <26.1 {
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FieldGuidePhotographWidget extends AbstractButton {
    private final Rect2i exposureArea;
    private final Supplier<ItemStack> photographGetter;
    private final Runnable onLeftClick;
    private final Runnable onRightClick;

    public FieldGuidePhotographWidget(int x, int y, int width, int height, Rect2i exposureArea,
                                      Supplier<ItemStack> photographGetter,
                                      Runnable onLeftClick, Runnable onRightClick,
                                      Component tooltipText) {
        super(x, y, width, height, Component.empty());
        this.exposureArea = exposureArea;
        this.photographGetter = photographGetter;
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;

        this.setTooltip(Tooltip.create(tooltipText));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ItemStack photograph = photographGetter.get();
        if (photograph.getItem() instanceof PhotographItem) {
            PhotographStyle style = PhotographStyle.of(photograph);

            // Paper
            guiGraphics.blit(style.albumPaperTexture(), getX(), getY(), 0, 0, width, height, width, height);

            // Exposure
            guiGraphics.pose().pushPose();
            float scale = exposureArea.getWidth();
            guiGraphics.pose().translate(exposureArea.getX(), exposureArea.getY(), 1);
            guiGraphics.pose().scale(scale, scale, scale);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            ExposureClient.photographRenderer().render(photograph, false, false, guiGraphics.pose(),
                    bufferSource, LightTexture.FULL_BRIGHT, 255, 255, 255, 255);
            bufferSource.endBatch();
            guiGraphics.pose().popPose();

            // Paper overlay
            if (style.hasAlbumOverlayTexture()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 2);
                guiGraphics.blit(style.albumOverlayTexture(), getX(), getY(), 0, 0, width, height, width, height);
                guiGraphics.pose().popPose();
            }
        }
    }

    @Override
    public void onPress() {
        onLeftClick.run();
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible
                && mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clicked(mouseX, mouseY)) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPress();
                return true;
            } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onRightClick.run();
                return true;
            }
        }
        return false;
    }

    @Override
    //? if >=1.21 {
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    //?} else {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        double scrollX = 0;
    *///?}
        if (scrollY > 0 && this.isHovered() && !photographGetter.get().isEmpty()) {
            this.onPress();
            return true;
        }
        //? if >=1.21 {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        //?} else {
        /*return super.mouseScrolled(mouseX, mouseY, scrollY);
        *///?}
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
//?}

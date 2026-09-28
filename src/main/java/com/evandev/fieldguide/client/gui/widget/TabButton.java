package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

//? if >=26.1 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

public class TabButton extends ImageButton {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_button"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_button_highlighted")
    );
    private final Category category;
    private final BookScreen parent;

    public TabButton(int x, int y, int width, int height, Category category, BookScreen parent) {
        super(x, y, width, height, SPRITES, (btn) -> parent.onTabClick(category));
        this.category = category;
        this.parent = parent;

        Component tooltipText = category.getId().getPath().equals("intro")
                ? Component.literal(ClientFieldGuideManager.getInstance().getJournalTitle())
                : Component.translatable(category.getTranslationKey());

        this.setTooltip(Tooltip.create(tooltipText));
    }

    @Override
    //? if <26.1 {
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void extractContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        boolean isSelected = (category == parent.getSelectedCategory());
        int vOffset = isSelected ? 24 : 0;

        GuiCompat.blit(guiGraphics, Constants.WIDGETS_TEXTURE, this.getX(), this.getY(), 0, 144 + vOffset, this.width, this.height, 256, 256);

        int iconX = this.getX() + 3;
        int iconY = this.getY() + 4;
        if (isSelected) iconX = iconX + 1;

        GuiCompat.blit(guiGraphics, category.getIcon(), iconX, iconY, 0, 0, 16, 16, 16, 16);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}

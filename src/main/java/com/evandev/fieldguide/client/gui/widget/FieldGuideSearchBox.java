package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

public class FieldGuideSearchBox extends EditBox {
    private static final int BORDER_COLOR_NORMAL = 0xFF51291D;
    private static final int BORDER_COLOR_FOCUSED = 0xFFDDC69B;
    private static final int BACKGROUND_COLOR = 0xFF050302;

    public FieldGuideSearchBox(Font font, int x, int y, int width, int height, String value, Consumer<String> onSearch) {
        super(font, x + 4, y + 6, width - 8, height - 8, Component.translatable("gui.fieldguide.search"));
        this.setMaxLength(50);
        this.setBordered(false);
        this.setVisible(true);
        //? if <26.1 {
        this.setTextColor(0xFCF3DD);
        //?} else {
        /*this.setTextColor(0xFFFCF3DD);
        *///?}
        this.setValue(value);
        this.setResponder(onSearch);
    }

    public void setInitialFocus() {
        this.setFocused(true);
        this.setEditable(true);
        this.setCursorPosition(this.getValue().length());
    }

    @Override
    //? if <26.1 {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.isActive() && this.isMouseOver(mouseX, mouseY)) {
    //?} else {
    /*public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && this.isActive() && this.isMouseOver(event.x(), event.y())) {
    *///?}
            this.setValue("");
            this.setFocused(true);
            return true;
        }

        //? if <26.1 {
        return super.mouseClicked(mouseX, mouseY, button);
        //?} else {
        /*return super.mouseClicked(event, doubleClick);
        *///?}
    }

    @Override
    //? if <26.1 {
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*public void extractWidgetRenderState(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        int boxX = this.getX() - 6;
        int boxY = this.getY() - 8;
        int boxW = this.width + 12;
        int boxH = this.height + 12;

        int borderColor = this.isFocused() ? BORDER_COLOR_FOCUSED : BORDER_COLOR_NORMAL;

        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, BACKGROUND_COLOR);

        //? if <26.1 {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        //?} else {
        /*super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
        *///?}

        if (this.getValue().isEmpty() && !this.isFocused()) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.fieldguide.search"), this.getX(), this.getY(), ClientConfig.get().getTextMutedColorInt(), false);
        }

        //? if <26.1 {
        guiGraphics.renderOutline(this.getX() - 5, this.getY() - 7, this.width + 10, this.height + 10, borderColor);
        //?} else {
        /*guiGraphics.outline(this.getX() - 5, this.getY() - 7, this.width + 10, this.height + 10, borderColor);
        *///?}
    }
}

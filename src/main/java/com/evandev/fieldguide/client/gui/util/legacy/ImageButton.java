package com.evandev.fieldguide.client.gui.util.legacy;

//? if <1.21 {
/*import com.evandev.fieldguide.client.gui.util.GuiCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ImageButton extends Button {
    protected final WidgetSprites sprites;

    public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress) {
        this(x, y, width, height, sprites, onPress, CommonComponents.EMPTY);
    }

    public ImageButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress, Component message) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.sprites = sprites;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.blitSprite(guiGraphics, this.sprites.get(this.isActive(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height);
    }
}
*///?}

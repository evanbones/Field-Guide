package com.evandev.fieldguide.client.gui.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;

public class PageTurnButton extends ImageButton {

    public PageTurnButton(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}

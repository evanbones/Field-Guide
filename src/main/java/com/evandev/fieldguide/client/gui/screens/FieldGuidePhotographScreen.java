package com.evandev.fieldguide.client.gui.screens;

//? if <26.1 {
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class FieldGuidePhotographScreen extends PhotographScreen {
    private final Screen parentScreen;

    public FieldGuidePhotographScreen(Screen parentScreen, List<ItemAndStack<PhotographItem>> photographs) {
        super(photographs);
        this.parentScreen = parentScreen;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
    }
}
//?}

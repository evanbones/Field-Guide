package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.platform.services.IClientHelper;
import net.minecraft.client.Minecraft;

public class NeoForgeClientHelper implements IClientHelper {

    @Override
    public void openFieldGuide() {
        Minecraft.getInstance().gui.setScreen(new FieldGuideCategoryScreen(null));
    }
}

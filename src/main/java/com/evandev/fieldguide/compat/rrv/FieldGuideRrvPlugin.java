package com.evandev.fieldguide.compat.rrv;

//? if >=26.1 {
/*import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import cc.cassian.rrv.api.overlay.OverlayView;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class FieldGuideRrvPlugin implements ReliableRecipeViewerClientPlugin {

    @Override
    public void onIntegrationInitialize() {
        OverlayView.registerGlobalOverlayKeybindSlotHandler((event, slot, overlay) -> {
            if (FieldGuideClient.OPEN_GUIDE_KEY == null || !FieldGuideClient.OPEN_GUIDE_KEY.matches(event)) return false;

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) return false;

            Minecraft mc = Minecraft.getInstance();
            FieldGuideCategoryScreen screen = new FieldGuideCategoryScreen("=^" + stack.getHoverName().getString().toLowerCase(Locale.ROOT), mc.screen);
            screen.setSearchItemStack(stack);
            mc.setScreen(screen);
            return true;
        });
    }
}
*///?}

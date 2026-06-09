package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.compat.emi.EmiCompat;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void fieldguide$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (((Screen) (Object) this).getFocused() instanceof EditBox) {
            return;
        }

        if (FieldGuideClient.OPEN_GUIDE_KEY != null && FieldGuideClient.OPEN_GUIDE_KEY.matches(keyCode, scanCode)) {
            if (Services.PLATFORM.isModLoaded("emi")) {
                ItemStack hoveredStack = EmiCompat.getHoveredItem();

                if (hoveredStack != null && !hoveredStack.isEmpty()) {
                    String query = "=^" + hoveredStack.getHoverName().getString().toLowerCase(Locale.ROOT);
                    FieldGuideCategoryScreen screen = new FieldGuideCategoryScreen(query, (Screen) (Object) this);
                    screen.setSearchItemStack(hoveredStack);
                    Minecraft.getInstance().setScreen(screen);
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
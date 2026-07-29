package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.config.ClientConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {

    @Unique
    private static final WidgetSprites PAUSE_BUTTON_SPRITES = new WidgetSprites(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_button"), Identifier.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_button_highlighted"));

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    private void addFieldGuideButton(CallbackInfo ci) {
        ClientConfig config = ClientConfig.get();
        if (!config.showPauseMenuButton) {
            return;
        }

        int buttonSize = 20;
        int margin = 4;

        Component message = Component.translatable("gui.fieldguide.open");

        int columnX = this.width / 2 - 102;
        int targetY = 0;

        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                if (widget.getX() == columnX && widget.getY() > targetY) {
                    targetY = widget.getY();
                }
            }
        }

        if (targetY == 0) {
            targetY = this.height / 4 + 120 + 24;
        }

        int finalX = (columnX - buttonSize - margin) + config.pauseButtonXOffset;
        int finalY = targetY + config.pauseButtonYOffset;

        ImageButton guideButton = new ImageButton(finalX, finalY, buttonSize, buttonSize, PAUSE_BUTTON_SPRITES, (button) -> {
            if (!FieldGuideClient.canOpenGuide()) return;
            String defaultMode = ClientConfig.get().defaultScreen;
            if ("last_opened_screen".equals(defaultMode) && BookScreen.lastOpenedScreen != null) {
                this.minecraft.gui.setScreen(BookScreen.lastOpenedScreen);
            } else {
                this.minecraft.gui.setScreen(new FieldGuideCategoryScreen());
            }
        },
                message
        );

        this.addRenderableWidget(guideButton);
    }
}
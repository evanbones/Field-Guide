package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {

    @Unique
    private static final WidgetSprites GUIDE_BUTTON_SPRITES = new WidgetSprites(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_inventory_button"), Identifier.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_inventory_button_highlighted"));

    @Unique
    private ImageButton fieldguide$guideButton;

    public InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addFieldGuideButton(CallbackInfo ci) {
        if (!ClientConfig.get().showInventoryButton) {
            return;
        }

        int xPos = this.leftPos + ClientConfig.get().inventoryButtonXOffset;
        int yPos = this.topPos + ClientConfig.get().inventoryButtonYOffset;

        this.fieldguide$guideButton = new ImageButton(xPos, yPos, 20, 18, GUIDE_BUTTON_SPRITES, (button) -> {
            if (!FieldGuideClient.canOpenGuide()) return;
            String defaultMode = ClientConfig.get().defaultScreen;
            if ("last_opened_screen".equals(defaultMode) && BookScreen.lastOpenedScreen != null) {
                this.minecraft.gui.setScreen(BookScreen.lastOpenedScreen);
            } else {
                this.minecraft.gui.setScreen(new FieldGuideCategoryScreen());
            }
        }
        );

        this.addRenderableWidget(this.fieldguide$guideButton);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"))
    private void updateButtonPosition(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.fieldguide$guideButton != null && ClientConfig.get().showInventoryButton) {
            this.fieldguide$guideButton.setX(this.leftPos + ClientConfig.get().inventoryButtonXOffset);
            this.fieldguide$guideButton.setY(this.topPos + ClientConfig.get().inventoryButtonYOffset);
        }
    }
}
package com.evandev.fieldguide.mixin.client;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <26.1 {
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
//?}

//? if >=26.1 {
/*import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
*///?}

@Mixin(InventoryScreen.class)
//? if <26.1 {
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
//?} else {
/*public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
*///?}

    @Unique
    private static final WidgetSprites GUIDE_BUTTON_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_inventory_button"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/fieldguide_inventory_button_highlighted"));

    @Unique
    private ImageButton fieldguide$guideButton;

    public InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        //? if <26.1 {
        super(menu, inventory, title);
        //?} else {
        /*super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
        *///?}
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addFieldGuideButton(CallbackInfo ci) {
        if (!ClientConfig.get().showInventoryButton) {
            return;
        }

        int xPos = this.leftPos + ClientConfig.get().inventoryButtonXOffset;
        int yPos = this.topPos + ClientConfig.get().inventoryButtonYOffset;

        this.fieldguide$guideButton = new ImageButton(xPos, yPos, 20, 18, GUIDE_BUTTON_SPRITES, (button) -> {
            if (FieldGuideClient.canOpenGuide()) FieldGuideClient.openDefaultScreen();
        });

        this.addRenderableWidget(this.fieldguide$guideButton);
    }

    //? if <26.1 {
    @Inject(method = "render", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    *///?}
    private void updateButtonPosition(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.fieldguide$guideButton != null && ClientConfig.get().showInventoryButton) {
            this.fieldguide$guideButton.setX(this.leftPos + ClientConfig.get().inventoryButtonXOffset);
            this.fieldguide$guideButton.setY(this.topPos + ClientConfig.get().inventoryButtonYOffset);
        }
    }
}

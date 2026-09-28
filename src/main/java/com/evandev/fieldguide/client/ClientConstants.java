package com.evandev.fieldguide.client;

import com.evandev.fieldguide.Constants;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;

public class ClientConstants {
    public static final WidgetSprites PREV_PAGE_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/page_prev"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/page_prev_highlighted"));
    public static final WidgetSprites NEXT_PAGE_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/page_next"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/page_next_highlighted"));
    public static final WidgetSprites BACK_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/back_button"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/back_button_highlighted"));

    public static final WidgetSprites PREV_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_prev"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_prev_disabled"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_prev_highlighted"));
    public static final WidgetSprites NEXT_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_next"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_next_disabled"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_next_highlighted"));

    public static final WidgetSprites TAB_UP_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_up"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_up_highlighted")
    );
    public static final WidgetSprites TAB_DOWN_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_down"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/tab_down_highlighted")
    );

    public static final WidgetSprites COPY_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/copy_button"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/copy_button_disabled"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/copy_button_highlighted"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/copy_button_disabled")
    );
}

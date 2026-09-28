package com.evandev.fieldguide.item;

import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

//? if >=26.1 {
/*import com.evandev.fieldguide.Constants;
*///?}

public class ModItems {

    public static Supplier<FieldGuideItem> FIELD_GUIDE;
    public static Supplier<Item> LENS;
    public static Supplier<PageItem> PAGE;

    public static void init() {
        if (ServerConfig.get().enableFieldGuideItem) {
            //? if <26.1 {
            FIELD_GUIDE = Services.REGISTRY.registerItem("field_guide", () -> new FieldGuideItem(new Item.Properties().stacksTo(1)));
            //?} else {
            /*FIELD_GUIDE = Services.REGISTRY.registerItem("field_guide", () -> new FieldGuideItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "field_guide"))).stacksTo(1)));
            *///?}
            Services.REGISTRY.registerToTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities")), FIELD_GUIDE);
        }
        if (ServerConfig.get().enableLensItem) {
            //? if <26.1 {
            LENS = Services.REGISTRY.registerItem("lens", () -> new Item(new Item.Properties().stacksTo(1)));
            //?} else {
            /*LENS = Services.REGISTRY.registerItem("lens", () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "lens"))).stacksTo(1)));
            *///?}
            Services.REGISTRY.registerToTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities")), LENS);
        }
        //? if <26.1 {
        PAGE = Services.REGISTRY.registerItem("page", () -> new PageItem(new Item.Properties().stacksTo(64)));
        //?} else {
        /*PAGE = Services.REGISTRY.registerItem("page", () -> new PageItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "page"))).stacksTo(64)));
        *///?}
        Services.REGISTRY.registerToTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities")), PAGE);
    }
}

package com.evandev.fieldguide.item;

import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {

    public static Supplier<FieldGuideItem> FIELD_GUIDE;
    public static Supplier<PageItem> PAGE;

    public static void init() {
        if (ServerConfig.get().enableFieldGuideItem) {
            FIELD_GUIDE = Services.REGISTRY.registerItem("field_guide", () -> new FieldGuideItem(new Item.Properties().stacksTo(1)));
            Services.REGISTRY.registerToTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("minecraft", "tools_and_utilities")), FIELD_GUIDE);
        }
        PAGE = Services.REGISTRY.registerItem("page", () -> new PageItem(new Item.Properties().stacksTo(64)));
        Services.REGISTRY.registerToTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("minecraft", "tools_and_utilities")), PAGE);
    }
}

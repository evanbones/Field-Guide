package com.evandev.fieldguide.api.attribute;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record GuideAttribute(ResourceLocation icon, int u, int v, int width, int height, int textureWidth,
                             int textureHeight, @Nullable String value, Component tooltip) {
    public GuideAttribute(ResourceLocation icon, @Nullable String value, Component tooltip) {
        this(icon, 0, 0, 16, 16, 16, 16, value, tooltip);
    }

    public static GuideAttribute of(ResourceLocation icon, @Nullable String value, Component tooltip) {
        return new GuideAttribute(icon, 0, 0, 16, 16, 16, 16, value, tooltip);
    }

    public static GuideAttribute of(ResourceLocation icon, int u, int v, int width, int height, int textureWidth, int textureHeight, @Nullable String value, Component tooltip) {
        return new GuideAttribute(icon, u, v, width, height, textureWidth, textureHeight, value, tooltip);
    }
}

package com.evandev.fieldguide.client.gui.util.legacy;

//? if <1.21 {
/*import net.minecraft.resources.ResourceLocation;

public record WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused, ResourceLocation disabledFocused) {
    public WidgetSprites(ResourceLocation enabled, ResourceLocation enabledFocused) {
        this(enabled, enabled, enabledFocused, enabledFocused);
    }

    public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused) {
        this(enabled, disabled, enabledFocused, disabled);
    }

    public ResourceLocation get(boolean enabled, boolean focused) {
        if (enabled) {
            return focused ? this.enabledFocused : this.enabled;
        }
        return focused ? this.disabledFocused : this.disabled;
    }
}
*///?}

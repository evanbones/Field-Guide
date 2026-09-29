package com.evandev.fieldguide.util;

//? if <1.21 {
/*import net.minecraft.resources.ResourceLocation;

public final class IdCompat {
    private IdCompat() {
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String id) {
        return new ResourceLocation(id);
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, path);
    }
}
*///?}

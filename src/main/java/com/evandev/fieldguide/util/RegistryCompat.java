package com.evandev.fieldguide.util;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
//?}

public final class RegistryCompat {
    private RegistryCompat() {
    }

    public static <E> Registry<E> registry(RegistryAccess access, ResourceKey<? extends Registry<? extends E>> key) {
        //? if <26.1 {
        return access.registryOrThrow(key);
        //?} else {
        /*return access.lookupOrThrow(key);
        *///?}
    }

    public static boolean isFood(Item item) {
        //? if >=1.21 {
        return item.components().has(DataComponents.FOOD);
        //?} else {
        /*return item.isEdible();
        *///?}
    }

    public static ResourceLocation id(ResourceKey<?> key) {
        //? if <26.1 {
        return key.location();
        //?} else {
        /*return key.identifier();
        *///?}
    }
}

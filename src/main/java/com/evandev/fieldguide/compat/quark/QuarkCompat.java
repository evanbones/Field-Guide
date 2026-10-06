package com.evandev.fieldguide.compat.quark;

//? if forgelike && <26.1 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.violetmoon.quark.content.mobs.entity.Forgotten;

public final class QuarkCompat {
    private static final ResourceLocation FORGOTTEN_HAT = ResourceLocation.fromNamespaceAndPath("quark", "forgotten_hat");

    private QuarkCompat() {
    }

    public static void prepareDisplayEntity(Entity entity) {
        if (entity instanceof Forgotten forgotten) {
            forgotten.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.get(FORGOTTEN_HAT)));
            forgotten.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            forgotten.getEntityData().set(Forgotten.SHEATHED_ITEM, new ItemStack(Items.IRON_SWORD));
        }
    }
}
//?}

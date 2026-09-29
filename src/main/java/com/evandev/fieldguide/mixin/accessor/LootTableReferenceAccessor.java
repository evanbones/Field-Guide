package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(maxVersion = "1.20.4")
//? if >=1.21 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.entries.LootTableReference", remap = false)
//?} else {
/*@Mixin(LootTableReference.class)
*///?}
public interface LootTableReferenceAccessor {
    //? if <1.21 {
    /*@Accessor("name")
    ResourceLocation fieldguide$getName();
    *///?}
}

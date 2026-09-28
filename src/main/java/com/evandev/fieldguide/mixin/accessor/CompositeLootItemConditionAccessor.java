package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(minVersion = "26.3")
//? if <26.3 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition", remap = false)
//?} else {
/*@Mixin(CompositeLootItemCondition.class)
*///?}
public interface CompositeLootItemConditionAccessor {
    //? if >=26.3 {
    /*@Accessor("terms")
    HolderSet<LootItemCondition> fieldguide$getTerms();
    *///?}
}

package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(minVersion = "26.3")
//? if <26.3 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.functions.SequenceFunction", remap = false)
//?} else {
/*@Mixin(SequenceFunction.class)
*///?}
public interface SequenceFunctionAccessor {
    //? if >=26.3 {
    /*@Accessor("functions")
    HolderSet<LootItemFunction> fieldguide$getFunctions();
    *///?}
}

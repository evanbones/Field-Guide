package com.evandev.fieldguide.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if <26.3 {
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.List;
//?}

//? if >=26.3 {
/*import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
*///?}

//? if <26.3 {
@Mixin(LootPoolSingletonContainer.class)
//?} else {
/*@Mixin(UniformContainerBase.class)
*///?}
public interface LootPoolSingletonContainerAccessor {
    @Accessor("weight")
    int fieldguide$getWeight();

    //? if >=1.21 && <26.3 {
    @Accessor("functions")
    List<LootItemFunction> fieldguide$getFunctions();
    //?} else if <1.21 {
    /*@Accessor("functions")
    LootItemFunction[] fieldguide$getFunctions();
    *///?}
}

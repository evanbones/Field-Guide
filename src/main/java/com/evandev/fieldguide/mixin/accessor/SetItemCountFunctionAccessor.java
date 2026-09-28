package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if <26.3 {
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
//?}

//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
*///?}

@Mixin(SetItemCountFunction.class)
public interface SetItemCountFunctionAccessor {
    //? if <26.1 {
    @Accessor("value")
    NumberProvider fieldguide$getValue();
    //?} else if <26.3 {
    /*@Accessor("count")
    NumberProvider fieldguide$getCount();
    *///?} else {
    /*@Accessor("count")
    Holder<ContextIntProvider> fieldguide$getCount();
    *///?}
}

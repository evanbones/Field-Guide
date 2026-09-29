package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21 {
/*import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(maxVersion = "1.20.4")
//? if >=1.21 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.providers.number.ConstantValue", remap = false)
//?} else {
/*@Mixin(ConstantValue.class)
*///?}
public interface ConstantValueAccessor {
    //? if <1.21 {
    /*@Accessor("value")
    float fieldguide$getValue();
    *///?}
}

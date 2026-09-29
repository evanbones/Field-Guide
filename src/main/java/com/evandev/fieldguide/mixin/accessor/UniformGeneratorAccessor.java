package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21 {
/*import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(maxVersion = "1.20.4")
//? if >=1.21 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.providers.number.UniformGenerator", remap = false)
//?} else {
/*@Mixin(UniformGenerator.class)
*///?}
public interface UniformGeneratorAccessor {
    //? if <1.21 {
    /*@Accessor("min")
    NumberProvider fieldguide$getMin();

    @Accessor("max")
    NumberProvider fieldguide$getMax();
    *///?}
}

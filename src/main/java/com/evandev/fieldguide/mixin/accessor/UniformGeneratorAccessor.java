package com.evandev.fieldguide.mixin.accessor;

//? if <1.21 {
/*import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UniformGenerator.class)
public interface UniformGeneratorAccessor {
    @Accessor("min")
    NumberProvider fieldguide$getMin();

    @Accessor("max")
    NumberProvider fieldguide$getMax();
}
*///?}

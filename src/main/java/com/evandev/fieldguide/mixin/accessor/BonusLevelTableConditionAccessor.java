package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21 {
/*import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(maxVersion = "1.20.4")
//? if >=1.21 {
@Mixin(targets = "net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition", remap = false)
//?} else {
/*@Mixin(BonusLevelTableCondition.class)
*///?}
public interface BonusLevelTableConditionAccessor {
    //? if <1.21 {
    /*@Accessor("values")
    float[] fieldguide$getValues();
    *///?}
}

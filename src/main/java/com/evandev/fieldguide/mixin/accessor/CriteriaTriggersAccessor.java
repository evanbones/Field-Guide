package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21 {
/*import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.gen.Invoker;
*///?}

@IfMinecraftVersion(maxVersion = "1.20.1")
//? if >=1.21 {
@Mixin(targets = "net.minecraft.advancements.CriteriaTriggers", remap = false)
//?} else {
/*@Mixin(CriteriaTriggers.class)
*///?}
public interface CriteriaTriggersAccessor {
    //? if <1.21 {
    /*@Invoker("register")
    static <T extends CriterionTrigger<?>> T fieldguide$register(T trigger) {
        throw new AssertionError();
    }
    *///?}
}

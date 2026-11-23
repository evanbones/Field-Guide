package com.evandev.fieldguide.mixin.accessor;

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SequenceFunction.class)
public interface SequenceFunctionAccessor {
    @Accessor("functions")
    HolderSet<LootItemFunction> fieldguide$getFunctions();
}
*///?}

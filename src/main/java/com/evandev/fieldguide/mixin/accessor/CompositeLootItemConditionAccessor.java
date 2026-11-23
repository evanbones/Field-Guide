package com.evandev.fieldguide.mixin.accessor;

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeLootItemCondition.class)
public interface CompositeLootItemConditionAccessor {
    @Accessor("terms")
    HolderSet<LootItemCondition> fieldguide$getTerms();
}
*///?}

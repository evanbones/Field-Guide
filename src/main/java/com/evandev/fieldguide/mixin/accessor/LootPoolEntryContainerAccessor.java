package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if <26.3 {
import java.util.List;
//?}

//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.Optional;
*///?}

@Mixin(LootPoolEntryContainer.class)
public interface LootPoolEntryContainerAccessor {
    //? if <26.3 {
    @Accessor("conditions")
    List<LootItemCondition> fieldguide$getConditions();
    //?} else {
    /*@Accessor("condition")
    Optional<Holder<LootItemCondition>> fieldguide$getCondition();

    @Accessor("modifier")
    Optional<Holder<LootItemFunction>> fieldguide$getModifier();
    *///?}
}

package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//? if <26.3 {
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
//?}

//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

import java.util.Optional;
*///?}

@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("rolls")
    //? if <26.3 {
    NumberProvider fieldguide$getRolls();
    //?} else {
    /*Holder<ContextIntProvider> fieldguide$getRolls();
    *///?}

    @Accessor("entries")
    List<LootPoolEntryContainer> fieldguide$getEntries();

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

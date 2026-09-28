package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if <26.3 {
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceKey;
//?}

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
*///?}

@Mixin(NestedLootTable.class)
public interface NestedLootTableAccessor {
    //? if <26.3 {
    @Accessor("contents")
    Either<ResourceKey<LootTable>, LootTable> fieldguide$getContents();
    //?} else {
    /*@Accessor("value")
    HolderSet<LootTable> fieldguide$getValue();
    *///?}
}

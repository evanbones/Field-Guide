package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21 {
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

//? if >=1.21 && <26.3 {
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceKey;
//?}

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
*///?}

@IfMinecraftVersion(minVersion = "1.20.5")
//? if >=1.21 {
@Mixin(NestedLootTable.class)
//?} else {
/*@Mixin(targets = "net.minecraft.world.level.storage.loot.entries.NestedLootTable", remap = false)
*///?}
public interface NestedLootTableAccessor {
    //? if >=1.21 && <26.3 {
    @Accessor("contents")
    Either<ResourceKey<LootTable>, LootTable> fieldguide$getContents();
    //?} else if >=26.3 {
    /*@Accessor("value")
    HolderSet<LootTable> fieldguide$getValue();
    *///?}
}

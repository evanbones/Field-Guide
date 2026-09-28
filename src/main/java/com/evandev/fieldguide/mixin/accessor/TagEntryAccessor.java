package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if <26.3 {
import net.minecraft.tags.TagKey;
//?}

//? if >=26.3 {
/*import net.minecraft.core.HolderSet;
*///?}

@Mixin(TagEntry.class)
public interface TagEntryAccessor {
    @Accessor("tag")
    //? if <26.3 {
    TagKey<Item> fieldguide$getTag();
    //?} else {
    /*HolderSet<Item> fieldguide$getTag();
    *///?}
}

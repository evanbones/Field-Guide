package com.evandev.fieldguide.mixin.accessor;

//? if <1.21 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTableReference.class)
public interface LootTableReferenceAccessor {
    @Accessor("name")
    ResourceLocation fieldguide$getName();
}
*///?}

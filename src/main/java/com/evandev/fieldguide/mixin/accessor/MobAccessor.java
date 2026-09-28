package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobAccessor {
    @Invoker("getAmbientSound")
    @Nullable SoundEvent fieldguide$callGetAmbientSound();
}

package com.evandev.fieldguide.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("wasTouchingWater")
    void fieldguide$setWasTouchingWater(boolean wasTouchingWater);

    @Accessor("id")
    int fieldguide$rawId();
}
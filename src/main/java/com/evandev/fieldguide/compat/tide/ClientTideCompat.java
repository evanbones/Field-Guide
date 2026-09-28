package com.evandev.fieldguide.compat.tide;

import com.evandev.fieldguide.mixin.accessor.EntityAccessor;
import net.minecraft.world.entity.Entity;

public class ClientTideCompat {

    public static void applyLavaFishFix(Entity entity) {
        if (entity.getClass().getName().contains("tide")) {
            ((EntityAccessor) entity).fieldguide$setWasTouchingWater(true);
        }
    }
}

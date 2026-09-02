package com.evandev.fieldguide.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public final class DummyEntities {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1);

    private DummyEntities() {
    }

    @Nullable
    public static Entity create(EntityType<?> type, Level level, EntitySpawnReason reason) {
        Entity entity = type.create(level, reason);
        if (entity != null) {
            entity.setId(NEXT_ID.getAndUpdate(id -> id == Integer.MIN_VALUE ? -1 : id - 1));
        }
        return entity;
    }
}

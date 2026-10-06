package com.evandev.fieldguide.util;

//? if forgelike && <26.1 {
import com.evandev.fieldguide.compat.quark.QuarkCompat;
import com.evandev.fieldguide.platform.Services;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

//? if >=26.1 {
/*import net.minecraft.world.entity.EntitySpawnReason;

import java.util.concurrent.atomic.AtomicInteger;
*///?}

public final class DummyEntities {
    //? if >=26.1 {
    /*private static final AtomicInteger NEXT_ID = new AtomicInteger(-1);
    *///?}

    private DummyEntities() {
    }

    @Nullable
    public static Entity create(EntityType<?> type, Level level) {
        //? if <26.1 {
        Entity entity = type.create(level);
        //? if forgelike {
        if (entity != null && Services.PLATFORM.isModLoaded("quark")) {
            QuarkCompat.prepareDisplayEntity(entity);
        }
        //?}
        return entity;
        //?} else {
        /*Entity entity = type.create(level, EntitySpawnReason.LOAD);
        if (entity != null) {
            entity.setId(NEXT_ID.getAndUpdate(id -> id == Integer.MIN_VALUE ? -1 : id - 1));
        }
        return entity;
        *///?}
    }
}

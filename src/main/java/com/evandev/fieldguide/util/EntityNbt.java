package com.evandev.fieldguide.util;

//? if <26.1 {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class EntityNbt {
    private EntityNbt() {
    }

    public static CompoundTag save(Entity entity) {
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        return tag;
    }

    public static void load(Entity entity, CompoundTag tag) {
        entity.load(tag);
    }
}
//?} else {
/*import com.evandev.fieldguide.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class EntityNbt {
    private EntityNbt() {
    }

    public static CompoundTag save(Entity entity) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            Leashable.LeashData invalidLeashData = null;
            if (entity instanceof Leashable leashable) {
                Leashable.LeashData data = leashable.getLeashData();
                if (data != null && data.leashHolder == null && data.delayedLeashInfo == null) {
                    invalidLeashData = data;
                    leashable.setLeashData(null);
                }
            }
            try {
                entity.saveWithoutId(output);
            } finally {
                if (invalidLeashData != null) ((Leashable) entity).setLeashData(invalidLeashData);
            }
            return output.buildResult();
        }
    }

    public static void load(Entity entity, CompoundTag tag) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            entity.load(TagValueInput.create(reporter, entity.registryAccess(), tag));
        }
    }
}
*///?}

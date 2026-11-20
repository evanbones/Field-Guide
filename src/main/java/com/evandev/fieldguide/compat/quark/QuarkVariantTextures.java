package com.evandev.fieldguide.compat.quark;

//? if neoforge && <26.1 {
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.allay.Allay;
import org.violetmoon.quark.content.client.module.VariantAnimalTexturesModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

public final class QuarkVariantTextures {
    private static final Set<Entity> STYLED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static void register() {
        FieldGuideVariantManager.registerProvider(Cow.class, new Provider<>(EntityType.COW, () -> VariantAnimalTexturesModule.enableCow,
                "black_and_white", "brown_and_white", "chestnut", "black"));
        FieldGuideVariantManager.registerProvider(Pig.class, new Provider<>(EntityType.PIG, () -> VariantAnimalTexturesModule.enablePig,
                "tan", "black", "rose"));
        FieldGuideVariantManager.registerProvider(Chicken.class, new Provider<>(EntityType.CHICKEN, () -> VariantAnimalTexturesModule.enableChicken,
                "brown", "gray", "dark_brown", "black", "silver", "cream"));
        FieldGuideVariantManager.registerProvider(Allay.class, new Provider<>(EntityType.ALLAY, () -> VariantAnimalTexturesModule.enableAllay,
                "green", "pink", "orange", "purple", "yellow"));
    }

    public static long displayIdentity(Entity entity, long fallback) {
        return STYLED.contains(entity) ? entity.getUUID().getMostSignificantBits() : fallback;
    }

    private record Style(EntityType<?> type, int index) {
    }

    private static final class Provider<T extends Mob> implements VariantProvider<T> {
        private final EntityType<?> type;
        private final BooleanSupplier enabled;
        private final List<VariantDef> variants;

        private Provider(EntityType<?> type, BooleanSupplier enabled, String... names) {
            this.type = type;
            this.enabled = enabled;
            List<VariantDef> list = new ArrayList<>();
            for (int i = 0; i < names.length; i++) {
                list.add(new VariantDef(names[i], new Style(type, i)));
            }
            list.add(new VariantDef("default", new Style(type, names.length)));
            this.variants = List.copyOf(list);
        }

        private boolean active(T entity) {
            return entity.getType() == type && VariantAnimalTexturesModule.staticEnabled && enabled.getAsBoolean()
                    && QuarkVanillaRenderers.drawsQuarkTexture(entity);
        }

        @Override
        public List<VariantDef> getVariants(T entity) {
            return active(entity) ? variants : List.of();
        }

        @Override
        public void apply(T entity, VariantDef def) {
            if (!active(entity) || !(def.value() instanceof Style style) || style.type() != type) return;
            entity.setUUID(new UUID(displayBits(style.index()), entity.getUUID().getLeastSignificantBits()));
            STYLED.add(entity);
        }

        @Override
        public VariantDef getCurrent(T entity) {
            if (!active(entity)) return new VariantDef("default", null);
            long most = entity.getUUID().getMostSignificantBits();
            return variants.get(Math.abs((int) (most % variants.size())));
        }

        @Override
        public String getCacheKey(T entity) {
            return entity.getClass().getName() + "_quark";
        }

        private long displayBits(int index) {
            int size = variants.size();
            int chance = VariantAnimalTexturesModule.shinyAnimalChance;
            int k = 1;
            long most = (long) size * k + index;
            while (chance > 0 && most % chance == 0 && k < 64) {
                k++;
                most = (long) size * k + index;
            }
            return most;
        }
    }
}
//?}

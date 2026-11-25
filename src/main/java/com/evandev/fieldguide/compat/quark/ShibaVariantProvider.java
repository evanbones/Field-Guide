package com.evandev.fieldguide.compat.quark;

import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.UUID;

public final class ShibaVariantProvider implements VariantProvider<Mob> {
    private static final String SHIBA_CLASS = "org.violetmoon.quark.content.mobs.entity.Shiba";
    private static final List<VariantDef> VARIANTS = List.of(
            new VariantDef("red", 3L),
            new VariantDef("black_and_tan", 1L),
            new VariantDef("sesame", 2L),
            new VariantDef("cream", 600L)
    );

    @SuppressWarnings("unchecked")
    public static void register() {
        try {
            Class<?> shiba = Class.forName(SHIBA_CLASS, false, ShibaVariantProvider.class.getClassLoader());
            if (Mob.class.isAssignableFrom(shiba)) {
                FieldGuideVariantManager.registerProvider((Class<Mob>) shiba, new ShibaVariantProvider());
            }
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
    }

    @Override
    public List<VariantDef> getVariants(Mob entity) {
        return VARIANTS;
    }

    @Override
    public void apply(Mob entity, VariantDef def) {
        if (def.value() instanceof Long least) {
            entity.setUUID(new UUID(entity.getUUID().getMostSignificantBits(), least));
        }
    }

    @Override
    public VariantDef getCurrent(Mob entity) {
        long least = Math.abs(entity.getUUID().getLeastSignificantBits());
        if (least % 200L == 0L) return VARIANTS.get(3);
        return VARIANTS.get((int) (least % 3L));
    }

    @Override
    public String getCacheKey(Mob entity) {
        return SHIBA_CLASS;
    }
}

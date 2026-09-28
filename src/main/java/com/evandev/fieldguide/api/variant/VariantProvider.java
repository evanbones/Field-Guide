package com.evandev.fieldguide.api.variant;

import net.minecraft.world.entity.Mob;

import java.util.List;

//? if >=26.1 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
*///?}

public interface VariantProvider<T extends Mob> {
    List<VariantDef> getVariants(T entity);

    void apply(T entity, VariantDef def);

    VariantDef getCurrent(T entity);

    default String getCacheKey(T entity) {
        return entity.getClass().getName();
    }

    default boolean suppressesDefaultVariant(T entity) {
        return false;
    }

    //? if >=26.1 {
    /*default void applyToRenderState(T entity, EntityRenderState state, VariantDef def) {
    }
    *///?}
}

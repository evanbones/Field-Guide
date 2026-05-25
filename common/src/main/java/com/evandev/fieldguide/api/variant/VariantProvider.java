package com.evandev.fieldguide.api.variant;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Mob;

import java.util.List;

public interface VariantProvider<T extends Mob> {
    List<VariantDef> getVariants(T entity);

    void apply(T entity, VariantDef def);

    VariantDef getCurrent(T entity);

    default String getCacheKey(T entity) {
        return entity.getClass().getName();
    }

    default void applyToRenderState(T entity, EntityRenderState state, VariantDef def) {
    }
}

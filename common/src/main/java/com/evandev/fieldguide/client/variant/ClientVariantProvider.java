package com.evandev.fieldguide.client.variant;

import com.evandev.fieldguide.api.variant.VariantDef;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Mob;

/**
 * Client-side companion to {@link com.evandev.fieldguide.api.variant.VariantProvider}.
 *
 * <p>Implement this interface alongside {@code VariantProvider} to hook into Field Guide's
 * entity rendering pipeline. {@link #postExtractRenderState} is called immediately after
 * {@code EntityRenderer.extractRenderState}, giving you a chance to write variant data
 * directly into the render state before the entity is submitted for rendering.</p>
 *
 */
public interface ClientVariantProvider<T extends Mob> {

    /**
     * Called after {@code EntityRenderer.extractRenderState} so implementors can inject
     * variant-specific data into the render state.
     *
     * @param entity  the entity being rendered
     * @param state   the extracted render state
     * @param variant the variant currently applied to the entity
     */
    default void postExtractRenderState(T entity, EntityRenderState state, VariantDef variant) {}
}

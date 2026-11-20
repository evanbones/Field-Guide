package com.evandev.fieldguide.compat.quark;

//? if neoforge && <26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public final class QuarkVanillaRenderers {
    private static final Set<Class<?>> VANILLA = Set.of(CowRenderer.class, PigRenderer.class, ChickenRenderer.class, AllayRenderer.class);

    public static boolean drawsQuarkTexture(Entity entity) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        return renderer != null && VANILLA.contains(renderer.getClass());
    }
}
//?}

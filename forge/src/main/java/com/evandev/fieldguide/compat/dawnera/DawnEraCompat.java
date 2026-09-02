package com.evandev.fieldguide.compat.dawnera;

import com.evandev.fieldguide.mixin.accessor.EntityRenderDispatcherAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import ru.astemir.astemirlib.client.bedrock.animation.Animated;
import ru.astemir.astemirlib.client.bedrock.model.BedrockEntityModel;
import ru.astemir.astemirlib.client.bedrock.renderer.BaseLivingRenderer;
import ru.astemir.astemirlib.client.bedrock.renderer.EntityRenderData;

public class DawnEraCompat {
    private static final String MOD_ID = "dawnera";

    @SuppressWarnings("unchecked")
    public static void preRender(Entity entity) {
        if (!ModList.get().isLoaded(MOD_ID)) return;

        if (entity instanceof Animated animated && entity instanceof LivingEntity living) {
            try {
                entity.tickCount = 100;
                entity.setInvisible(false);

                living.setHealth(living.getMaxHealth());
                living.deathTime = 0;
                living.hurtTime = 0;

                living.yBodyRot = 180.0F;
                living.setYRot(180.0F);
                living.yHeadRot = 180.0F;
                living.yHeadRotO = 180.0F;

                EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                if (renderer instanceof BaseLivingRenderer bedrockRenderer) {
                    BedrockEntityModel model = bedrockRenderer.getModel(living);
                    if (model != null) {
                        model.resetAll();
                        model.getRoot().setAllVisible();
                        model.updateAnimations((Entity) animated, EntityRenderData.prepare(entity, 0.0F));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean renderEntity(Entity entity, double x, double y, double z, float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!ModList.get().isLoaded(MOD_ID)) return false;

        if (entity instanceof LivingEntity living && entity instanceof Animated animated) {
            try {
                EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

                if (renderer instanceof BaseLivingRenderer bedrockRenderer) {
                    EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                    boolean shadow = ((EntityRenderDispatcherAccessor) dispatcher).fieldguide$shouldRenderShadow();
                    dispatcher.setRenderShadow(false);

                    Lighting.setupForEntityInInventory();

                    try {
                        bedrockRenderer.renderFinal(living, 0.0F, poseStack, bufferSource, 15728880);
                    } catch (Exception ignored) {
                    }

                    if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                        bs.endBatch();
                    }

                    dispatcher.setRenderShadow(shadow);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    public static void postRender(Entity entity) {
        if (ModList.get().isLoaded(MOD_ID) && entity instanceof Animated) {
            entity.setYRot(0.0F);
            entity.yRotO = 0.0F;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = 0.0F;
                living.yBodyRotO = 0.0F;
                living.yHeadRot = 0.0F;
                living.yHeadRotO = 0.0F;
            }
        }
    }
}

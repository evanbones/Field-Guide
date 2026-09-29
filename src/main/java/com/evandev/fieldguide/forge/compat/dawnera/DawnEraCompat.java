package com.evandev.fieldguide.forge.compat.dawnera;

//? if forge {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.astemir.astemirlib.client.bedrock.animation.Animated;
import ru.astemir.astemirlib.client.bedrock.model.BedrockEntityModel;
import ru.astemir.astemirlib.client.bedrock.renderer.BaseLivingRenderer;
import ru.astemir.astemirlib.client.bedrock.renderer.EntityRenderData;

public class DawnEraCompat {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void preRender(Entity entity) {
        if (!(entity instanceof Animated animated) || !(entity instanceof LivingEntity living)) return;

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

            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
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
*///?}

package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.compat.dawnera.DawnEraCompat;
import com.evandev.fieldguide.platform.services.IClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

public class ForgeClientHelper implements IClientHelper {

    @Override
    public void openFieldGuide() {
        FieldGuideClient.openGuide();
    }

    @Override
    public void preRenderEntity(Entity entity) {
        DawnEraCompat.preRender(entity);
    }

    @Override
    public boolean renderEntity(Entity entity, double x, double y, double z, float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return DawnEraCompat.renderEntity(entity, x, y, z, yRot, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void postRenderEntity(Entity entity) {
        DawnEraCompat.postRender(entity);
    }
}

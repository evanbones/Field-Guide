package com.evandev.fieldguide.mixin.client;

//? if neoforge && <26.1 {
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.violetmoon.quark.content.mobs.client.layer.forgotten.ForgottenClothingLayer;

// Quark passes 0xFFFFFF as an ARGB colour, so the clothing is written with alpha 0 and isn't visible in icons
// TODO: this should probably be a PR
@IfModLoaded("quark")
@Mixin(ForgottenClothingLayer.class)
public class ForgottenClothingLayerMixin {
    @ModifyConstant(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Mob;FFFFFF)V", constant = @Constant(intValue = 0xFFFFFF))
    private int fieldguide$opaqueColor(int color) {
        return color | 0xFF000000;
    }
}
//?}

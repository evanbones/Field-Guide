package com.evandev.fieldguide.mixin.client;

//? if <26.1 {
import com.evandev.fieldguide.client.render.LevelOverlays;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void skipNameTagInScanOverlay(CallbackInfo ci) {
        if (LevelOverlays.isRendering()) {
            ci.cancel();
        }
    }
}
//?}

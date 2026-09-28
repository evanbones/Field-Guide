package com.evandev.fieldguide.mixin.server;

import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "save", at = @At("TAIL"))
    private void fieldguide$save(ServerPlayer player, CallbackInfo ci) {
        FieldGuideProgressManager.getInstance().save(player);
    }
}

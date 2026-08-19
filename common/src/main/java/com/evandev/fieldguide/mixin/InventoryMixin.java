package com.evandev.fieldguide.mixin;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void onAddItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.get().disableObtainUnlocks && !stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation itemId = EntryResolver.getEntryId(stack.getItem());
            PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(serverPlayer);
            if (progress != null) {
                progress.tryUnlock(serverPlayer, itemId, null, EntryUnlockData.UnlockTrigger.OBTAIN);
            }
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (!ServerConfig.get().disableObtainUnlocks && !stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation itemId = EntryResolver.getEntryId(stack.getItem());
            PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(serverPlayer);
            if (progress != null) {
                progress.tryUnlock(serverPlayer, itemId, null, EntryUnlockData.UnlockTrigger.OBTAIN);
            }
        }
    }
}
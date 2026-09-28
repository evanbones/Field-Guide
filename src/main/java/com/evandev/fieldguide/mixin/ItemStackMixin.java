package com.evandev.fieldguide.mixin;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void onFinishUsingItem(Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
        //? if <26.1 {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer serverPlayer) {
        //?} else {
        /*if (!level.isClientSide() && livingEntity instanceof ServerPlayer serverPlayer) {
        *///?}
            ItemStack stack = (ItemStack) (Object) this;
            ResourceLocation itemId = EntryResolver.getEntryId(stack.getItem());
            PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(serverPlayer);
            if (progress != null) {
                progress.tryUnlock(serverPlayer, itemId, null, EntryUnlockData.UnlockTrigger.EAT);
            }
        }
    }
}

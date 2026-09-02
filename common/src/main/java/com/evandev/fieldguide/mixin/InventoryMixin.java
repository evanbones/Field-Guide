package com.evandev.fieldguide.mixin;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void onAddItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            fieldguide$tryUnlockItem(serverPlayer, stack);
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            fieldguide$tryUnlockItem(serverPlayer, stack);
        }
    }

    @Unique
    private void fieldguide$tryUnlockItem(ServerPlayer serverPlayer, ItemStack stack) {
        if (ServerConfig.get().disableObtainUnlocks) return;

        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(serverPlayer);
        if (progress == null) return;

        if (stack.is(Items.PAINTING) && stack.hasTag()) {
            CompoundTag entityTag = stack.getTagElement("EntityTag");
            if (entityTag != null) {
                Painting.loadVariant(entityTag).flatMap(holder -> holder.unwrapKey().map(ResourceKey::location)).ifPresent(variantId -> {
                    ResourceLocation prefixedId = new ResourceLocation("item", variantId.getNamespace() + "/" + variantId.getPath());
                    progress.tryUnlock(serverPlayer, prefixedId, null, EntryUnlockData.UnlockTrigger.OBTAIN);
                });
                return;
            }
        }

        ResourceLocation itemId = EntryResolver.getEntryId(stack.getItem());
        progress.tryUnlock(serverPlayer, itemId, null, EntryUnlockData.UnlockTrigger.OBTAIN);
    }
}

package com.evandev.fieldguide.mixin;

import com.evandev.fieldguide.item.ModItems;
import com.evandev.fieldguide.network.OpenGuidePacket;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <1.21 {
/*import net.minecraft.world.InteractionHand;
*///?}
//? if >=26.3 {
/*import net.minecraft.util.Prediction;
*///?}

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin {

    //? if <1.21 {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void fieldguide$useFieldGuide(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
    *///?} else {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void fieldguide$useFieldGuide(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
    //?}
        if (!state.getValue(LecternBlock.HAS_BOOK)) return;

        // The lectern's book isn't synced to clients, so only the server can tell it's a field guide
        //? if <26.1 {
        boolean clientSide = level.isClientSide;
        //?} else {
        /*boolean clientSide = level.isClientSide();
        *///?}
        if (clientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) || !fieldguide$isFieldGuide(lectern.getBook())) return;

        if (player.isSecondaryUseActive()) {
            ItemStack book = lectern.getBook();
            lectern.setBook(ItemStack.EMPTY);
            LecternBlock.resetBookState(player, level, pos, state, false);
            if (!player.getInventory().add(book)) {
                //? if <26.3 {
                player.drop(book, false);
                //?} else {
                /*player.drop(book, false, Prediction.SERVER_ONLY);
                *///?}
            }
        } else {
            Services.NETWORK.sendToPlayer(new OpenGuidePacket(), serverPlayer);
        }

        cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Unique
    private static boolean fieldguide$isFieldGuide(ItemStack stack) {
        return ModItems.FIELD_GUIDE != null && stack.is(ModItems.FIELD_GUIDE.get());
    }
}

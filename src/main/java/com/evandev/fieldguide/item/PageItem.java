package com.evandev.fieldguide.item;

import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

//? if <26.1 {
import net.minecraft.world.InteractionResultHolder;
//?}

//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
*///?}

public class PageItem extends Item {

    public PageItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if <26.1 {
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    //?} else {
    /*public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation entryId = ModDataComponents.get(stack, ModDataComponents.ENTRY_ID);

        if (entryId != null) {
            //? if <26.1 {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            //?} else {
            /*if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            *///?}
                PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(serverPlayer);

                if (progress != null) {
                    // Unlock base entry
                    progress.unlock(serverPlayer, entryId, null, false);

                    // Unlock variants
                    List<String> variants = ModDataComponents.get(stack, ModDataComponents.VARIANTS);
                    if (variants != null) {
                        Map<String, String> variantNames = ModDataComponents.get(stack, ModDataComponents.CUSTOM_VARIANT_NAMES);
                        for (String variant : variants) {
                            progress.unlock(serverPlayer, entryId, variant, false);

                            // Restore custom variant names
                            if (variantNames != null && variantNames.containsKey(variant)) {
                                progress.setCustomName(entryId + "#" + variant, variantNames.get(variant));
                            }
                        }
                    }

                    // Restore custom data
                    String customName = ModDataComponents.get(stack, ModDataComponents.CUSTOM_NAME);
                    if (customName != null) progress.setCustomName(entryId.toString(), customName);

                    String customDesc = ModDataComponents.get(stack, ModDataComponents.CUSTOM_DESCRIPTION);
                    if (customDesc != null) progress.setCustomDescription(entryId.toString(), customDesc);

                    String photograph = ModDataComponents.get(stack, ModDataComponents.PHOTOGRAPH);
                    if (photograph != null) progress.setEntryPhotograph(entryId.toString(), photograph);

                    Long discoveryTime = ModDataComponents.get(stack, ModDataComponents.DISCOVERY_TIME);
                    if (discoveryTime != null) progress.setDiscoveryTime(entryId.toString(), discoveryTime);

                    Long discoveryGameTime = ModDataComponents.get(stack, ModDataComponents.DISCOVERY_GAME_TIME);
                    if (discoveryGameTime != null) progress.setDiscoveryGameTime(entryId.toString(), discoveryGameTime);

                    progress.markEntryForResync(entryId.toString());

                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
            }
            //? if <26.1 {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            //?} else {
            /*return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            *///?}
        }

        //? if <26.1 {
        return InteractionResultHolder.pass(stack);
        //?} else {
        /*return InteractionResult.PASS;
        *///?}
    }

    @Override
    //? if >=1.21 && <26.1 {
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
    //?} else if <1.21 {
    /*public void appendHoverText(@NotNull ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
    *///?} else {
    /*public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
    *///?}
        Component entryName = ModDataComponents.get(stack, ModDataComponents.ENTRY_NAME);
        if (entryName != null) {
            //? if <26.1 {
            tooltipComponents.add(Component.translatable("item.fieldguide.page.entry", entryName).withStyle(ChatFormatting.GOLD));
            //?} else {
            /*tooltip.accept(Component.translatable("item.fieldguide.page.entry", entryName).withStyle(ChatFormatting.GOLD));
            *///?}
        }
        String author = ModDataComponents.get(stack, ModDataComponents.AUTHOR);
        if (author != null) {
            //? if <26.1 {
            tooltipComponents.add(Component.translatable("item.fieldguide.page.author", author).withStyle(ChatFormatting.GRAY));
            //?} else {
            /*tooltip.accept(Component.translatable("item.fieldguide.page.author", author).withStyle(ChatFormatting.GRAY));
            *///?}
        }
        //? if <26.1 {
        tooltipComponents.add(Component.translatable("item.fieldguide.page.tooltip").withStyle(ChatFormatting.BLUE));
        //?}
        //? if >=1.21 && <26.1 {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        //?} else if <1.21 {
        /*super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        *///?} else {
        /*tooltip.accept(Component.translatable("item.fieldguide.page.tooltip").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, display, tooltip, tooltipFlag);
        *///?}
    }
}

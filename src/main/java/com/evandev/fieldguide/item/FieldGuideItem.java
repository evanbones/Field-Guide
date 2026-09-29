package com.evandev.fieldguide.item;

import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.client.FieldGuideClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

//? if <26.1 {
import net.minecraft.world.InteractionResultHolder;

import java.util.List;
//?}

//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
*///?}

public class FieldGuideItem extends Item {

    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if <26.1 {
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    //?} else {
    /*public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand hand) {
    *///?}
        if (!ServerConfig.get().enableFieldGuideItem || ServerConfig.get().enableFieldGuideScanning) {
            //? if <26.1 {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
            //?} else {
            /*return InteractionResult.PASS;
            *///?}
        }

        //? if <26.1 {
        if (level.isClientSide) {
        //?} else {
        /*if (level.isClientSide()) {
        *///?}
            FieldGuideClient.openGuide();
        }
        //? if <26.1 {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        //?} else {
        /*return InteractionResult.SUCCESS;
        *///?}
    }

    @Override
    //? if >=1.21 && <26.1 {
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
    //?} else if <1.21 {
    /*public void appendHoverText(@NotNull ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
    *///?} else {
    /*public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
    *///?}
        if (ServerConfig.get().enableFieldGuideItem) {
            //? if <26.1 {
            tooltipComponents.add(Component.translatable("item.fieldguide.field_guide.tooltip").withStyle(ChatFormatting.GRAY));
            //?} else {
            /*tooltip.accept(Component.translatable("item.fieldguide.field_guide.tooltip").withStyle(ChatFormatting.GRAY));
            *///?}
        }
        //? if >=1.21 && <26.1 {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        //?} else if <1.21 {
        /*super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        *///?} else {
        /*super.appendHoverText(stack, context, display, tooltip, tooltipFlag);
        *///?}
    }
}

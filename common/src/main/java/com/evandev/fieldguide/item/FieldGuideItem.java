package com.evandev.fieldguide.item;

import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FieldGuideItem extends Item {

    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!ServerConfig.get().enableFieldGuideItem || ServerConfig.get().enableFieldGuideScanning) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (level.isClientSide) {
            Services.getClient().openFieldGuide();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (ServerConfig.get().enableFieldGuideItem) {
            tooltipComponents.add(Component.translatable("item.fieldguide.field_guide.tooltip").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

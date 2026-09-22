package com.evandev.fieldguide.compat.reliableremover;

import com.evandev.reliable_remover.api.ReliableRemoverAPI;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ReliableRemoverCompat {
    public static boolean isHidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return ReliableRemoverAPI.isItemHidden(stack) || ReliableRemoverAPI.isCreativeBlocked(stack);
    }

    public static boolean isHidden(Block block) {
        if (block == null) return false;
        ItemStack stack = new ItemStack(block);
        if (stack.isEmpty()) return false;
        return isHidden(stack);
    }
}
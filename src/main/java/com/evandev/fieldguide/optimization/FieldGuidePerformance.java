package com.evandev.fieldguide.optimization;

import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.ModDataComponents;
import java.util.*;
import net.minecraft.world.item.ItemStack;

public final class FieldGuidePerformance {
    private FieldGuidePerformance() {}
    public static boolean hasComposite(List<?> candidates) {
        for (Object value : candidates) if (value instanceof GuideEntry entry && entry.isComposite()) return true;
        return false;
    }
    /** Per-operation comparison copies only; returned stacks remain the original representatives. */
    public static List<ItemStack> distinctLoot(List<ItemStack> raw) {
        List<ItemStack> result = new ArrayList<>();
        ItemStack[] previousComparisons = null;
        for (ItemStack stack : raw) {
            if (stack == null) return originalDistinct(raw);
            ItemStack comparable = null;
            boolean duplicate = false;
            for (int i = 0; i < result.size(); i++) {
                ItemStack previous = result.get(i);
                if (!ItemStack.isSameItem(previous, stack)) continue;
                if (comparable == null) comparable = sanitized(stack);
                if (previousComparisons == null) previousComparisons = new ItemStack[raw.size()];
                if (previousComparisons[i] == null) previousComparisons[i] = sanitized(previous);
                if (ItemStack.isSameItemSameComponents(previousComparisons[i], comparable)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                if (previousComparisons != null) previousComparisons[result.size()] = comparable;
                result.add(stack);
            }
        }
        return result;
    }
    private static ItemStack sanitized(ItemStack stack) {
        ItemStack copy = stack.copy();
        ModDataComponents.remove(copy, ModDataComponents.DROP_CHANCE);
        ModDataComponents.remove(copy, ModDataComponents.MIN_DROP);
        ModDataComponents.remove(copy, ModDataComponents.MAX_DROP);
        return copy;
    }
    private static List<ItemStack> originalDistinct(List<ItemStack> raw) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : raw) {
            if (result.stream().noneMatch(previous -> ItemStack.isSameItem(previous, stack)
                    && ItemStack.isSameItemSameComponents(sanitized(previous), sanitized(stack)))) result.add(stack);
        }
        return result;
    }
}

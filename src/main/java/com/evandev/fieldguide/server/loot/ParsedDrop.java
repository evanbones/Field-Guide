package com.evandev.fieldguide.server.loot;

import net.minecraft.world.item.ItemStack;

public class ParsedDrop {
    public final ItemStack stack;
    public float chance;
    public int minCount;
    public int maxCount;

    public ParsedDrop(ItemStack stack, float chance, int min, int max) {
        this.stack = stack;
        this.chance = chance;
        this.minCount = min;
        this.maxCount = max;
    }
}

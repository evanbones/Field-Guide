package com.evandev.fieldguide.server.data;

import net.minecraft.world.item.ItemStack;

public record ItemStackKey(ItemStack stack) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStackKey(ItemStack stack1))) return false;
        return ItemStack.isSameItemSameComponents(this.stack, stack1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.stack);
    }
}

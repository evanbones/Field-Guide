package com.evandev.fieldguide.server.data;

import net.minecraft.world.item.ItemStack;

//? if <1.21 {
/*import java.util.Objects;
*///?}

public record ItemStackKey(ItemStack stack) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStackKey other)) return false;
        return ItemStack.isSameItemSameComponents(this.stack, other.stack);
    }

    @Override
    public int hashCode() {
        //? if >=1.21 {
        return ItemStack.hashItemAndComponents(this.stack);
        //?} else {
        /*return 31 * this.stack.getItem().hashCode() + Objects.hashCode(this.stack.getTag());
        *///?}
    }
}

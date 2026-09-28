package com.evandev.fieldguide.compat.emi;

//? if <26.1 {
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EmiCompat {

    public static ItemStack getHoveredItem() {
        EmiStackInteraction interaction = EmiApi.getHoveredStack(true);

        if (interaction != null && !interaction.isEmpty()) {
            List<EmiStack> emiStacks = interaction.getStack().getEmiStacks();

            if (!emiStacks.isEmpty()) {
                return emiStacks.get(0).getItemStack();
            }
        }

        return ItemStack.EMPTY;
    }
}
//?}

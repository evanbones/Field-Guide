package com.evandev.fieldguide.compat.trinkets;

import com.evandev.fieldguide.ModTags;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;

public class FabricTrinketsCompat {
    public static boolean hasSpyglass(Player player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(stack -> stack.is(ModTags.Items.SPYGLASSES)))
                .orElse(false);
    }
}
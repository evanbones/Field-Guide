package com.evandev.fieldguide.compat.curios;

import com.evandev.fieldguide.ModTags;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public class ForgeCuriosCompat {
    public static boolean hasSpyglass(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack -> stack.is(ModTags.Items.SPYGLASSES))
                .isPresent();
    }
}
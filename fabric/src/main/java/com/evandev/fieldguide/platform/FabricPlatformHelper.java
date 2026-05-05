package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.compat.trinkets.FabricTrinketsCompat;
import com.evandev.fieldguide.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean hasSpyglassEquipped(Player player) {
        if (player.isUsingItem() && player.getUseItem().is(ModTags.Items.SPYGLASSES)) {
            return true;
        }

        if (isModLoaded("trinkets")) {
            return FabricTrinketsCompat.hasSpyglass(player);
        }

        return false;
    }
}

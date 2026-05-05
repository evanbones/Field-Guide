package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.compat.curios.ForgeCuriosCompat;
import com.evandev.fieldguide.platform.services.IPlatformHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean hasSpyglassEquipped(Player player) {
        if (player.isUsingItem() && player.getUseItem().is(ModTags.Items.SPYGLASSES)) {
            return true;
        }

        if (isModLoaded("curios")) {
            return ForgeCuriosCompat.hasSpyglass(player);
        }

        return false;
    }
}

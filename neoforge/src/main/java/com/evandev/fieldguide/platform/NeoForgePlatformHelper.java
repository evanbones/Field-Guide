package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.compat.curios.NeoForgeCuriosCompat;
import com.evandev.fieldguide.compat.mixedlitter.MixedLitterCompat;
import com.evandev.fieldguide.platform.services.IPlatformHelper;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
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
    public void applyMixedLitterCompat(Entity entity) {
        if (entity instanceof Mob mob) {
            FieldGuideVariantManager.registerProvider((Class<Mob>) mob.getClass(), new MixedLitterCompat.MixedLitterVariantProvider());
        }
        MixedLitterCompat.applyDummyVariant(entity);
    }

    @Override
    public boolean hasSpyglassEquipped(Player player) {
        if (player.isUsingItem() && player.getUseItem().is(ModTags.Items.SPYGLASSES)) {
            return true;
        }

        if (isModLoaded("curios")) {
            return NeoForgeCuriosCompat.hasSpyglass(player);
        }

        return false;
    }
}
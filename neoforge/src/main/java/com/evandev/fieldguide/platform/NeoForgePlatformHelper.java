package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.compat.kubejs.FieldGuideKubeJSHooks;
import com.evandev.fieldguide.compat.mixedlitter.MixedLitterCompat;
import com.evandev.fieldguide.platform.services.IPlatformHelper;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
    public void onEntryUnlocked(ServerPlayer player, ResourceLocation entryId, String variantId, boolean newlyUnlocked, PlayerFieldGuideProgress progress) {
        if (isModLoaded("kubejs")) {
           FieldGuideKubeJSHooks.postEntryUnlocked(player, entryId, variantId, newlyUnlocked, progress);
        }
    }

    @Override
    public void onCategoryCompleted(ServerPlayer player, ResourceLocation categoryId, PlayerFieldGuideProgress progress) {
        if (isModLoaded("kubejs")) {
            FieldGuideKubeJSHooks.postCategoryCompleted(player, categoryId, progress);
        }
    }

    @Override
    public void applyMixedLitterCompat(Entity entity) {
        MixedLitterCompat.applyDummyVariant(entity);
    }
}
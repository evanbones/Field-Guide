package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

//? if fabric {
/*import net.fabricmc.loader.api.FabricLoader;
*///?} else {
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
//?}

//? if neoforge && <26.1 {
import com.evandev.fieldguide.neoforge.compat.mixedlitter.MixedLitterCompat;
//?}

//? if neoforge && <26.2 {
import com.evandev.fieldguide.neoforge.compat.kubejs.FieldGuideKubeJSHooks;
//?}

public class PlatformHelper {

    public boolean isModLoaded(String modId) {
        //? if fabric {
        /*return FabricLoader.getInstance().isModLoaded(modId);
        *///?} else {
        return ModList.get().isLoaded(modId);
        //?}
    }

    public Path getConfigDirectory() {
        //? if fabric {
        /*return FabricLoader.getInstance().getConfigDir();
        *///?} else {
        return FMLPaths.CONFIGDIR.get();
        //?}
    }

    public void applyMixedLitterCompat(Entity entity) {
        //? if neoforge && <26.1 {
        MixedLitterCompat.applyDummyVariant(entity);
        //?}
    }

    public void onEntryUnlocked(ServerPlayer player, ResourceLocation entryId, String variantId, boolean newlyUnlocked, PlayerFieldGuideProgress progress) {
        //? if neoforge && <26.2 {
        if (isModLoaded("kubejs")) {
            FieldGuideKubeJSHooks.postEntryUnlocked(player, entryId, variantId, newlyUnlocked, progress);
        }
        //?}
    }

    public void onCategoryCompleted(ServerPlayer player, ResourceLocation categoryId, PlayerFieldGuideProgress progress) {
        //? if neoforge && <26.2 {
        if (isModLoaded("kubejs")) {
            FieldGuideKubeJSHooks.postCategoryCompleted(player, categoryId, progress);
        }
        //?}
    }
}

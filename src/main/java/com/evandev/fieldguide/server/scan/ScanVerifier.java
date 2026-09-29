package com.evandev.fieldguide.server.scan;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.util.EntityNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class ScanVerifier {

    public static boolean verifyScan(ServerPlayer player, ResourceLocation entryId,
                                     ResourceLocation scannedTargetId, BlockPos targetBlockPos, int targetEntityId) {
        ServerConfig config = ServerConfig.getLocal();

        if (config.disableScanning) return false;

        double activeScanDist = 0;
        if (config.enableLensScanning) activeScanDist = Math.max(activeScanDist, config.lensScanDistance);
        if (config.enableSpyglassScanning) activeScanDist = Math.max(activeScanDist, config.spyglassScanDistance);
        if (config.enableNakedEyeScanning) activeScanDist = Math.max(activeScanDist, config.nakedEyeScanDistance);
        if (config.enableFieldGuideScanning) activeScanDist = Math.max(activeScanDist, config.fieldGuideScanDistance);
        if (activeScanDist <= 0) return false;

        double maxDistSq = activeScanDist * activeScanDist;
        //? if <26.1 {
        ServerLevel level = player.serverLevel();
        //?} else {
        /*ServerLevel level = player.level();
        *///?}

        ResourceLocation categoryId = ServerFieldGuideManager.getInstance().getCategoryForEntryId(entryId);
        if (categoryId == null) return false;

        if (targetEntityId != 0) {
            Entity entity = level.getEntity(targetEntityId);
            if (entity == null || entity.isSpectator() || player.distanceToSqr(entity) > maxDistSq) {
                return false;
            }

            if (FieldGuideCobblemonCompat.isPokemon(entity)) {
                ResourceLocation pokemonEntryId = FieldGuideCobblemonCompat.getPokemonEntryId(entity);
                if (targetBelongsToEntry(pokemonEntryId, entryId)) return true;
                return FieldGuideCobblemonCompat.getSpeciesName(pokemonEntryId).equals(FieldGuideCobblemonCompat.getSpeciesName(entryId));
            }

            GuideEntry resolvedEntry = ServerFieldGuideManager.getInstance().getResolvedEntry(entryId);
            if (resolvedEntry != null && resolvedEntry.targetEntityType() != null) {
                ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (!resolvedEntry.targetEntityType().equals(entityTypeId)) {
                    Constants.LOG.warn("verifyScan failed: Client tried to unlock {}, but scanned entity type {} doesn't match required {}.",
                            entryId, entityTypeId, resolvedEntry.targetEntityType());
                    return false;
                }
                if (resolvedEntry.nbtPredicate() != null && entity instanceof Mob mob) {
                    CompoundTag entityNbt = EntityNbt.save(mob);
                    return NbtUtils.compareNbt(resolvedEntry.nbtPredicate(), entityNbt, true);
                }
                return true;
            }

        } else if (targetBlockPos != null) {
            if (player.distanceToSqr(Vec3.atCenterOf(targetBlockPos)) > maxDistSq) {
                return false;
            }
        } else {
            return false;
        }

        boolean belongs = targetBelongsToEntry(scannedTargetId, entryId);
        if (!belongs) {
            Constants.LOG.warn("verifyScan failed: Client tried to unlock {}, but scanned {}.", entryId, scannedTargetId);
        }

        return belongs;
    }

    private static boolean targetBelongsToEntry(ResourceLocation scannedTargetId, ResourceLocation entryId) {
        if (scannedTargetId.equals(entryId)) return true;

        ResourceLocation rawScannedId = EntryResolver.getRawId(scannedTargetId);
        ResourceLocation rawEntryId = EntryResolver.getRawId(entryId);

        if (rawScannedId != null && rawScannedId.equals(rawEntryId)) return true;
        ResourceLocation redirected = ServerFieldGuideManager.getInstance().getRedirects().get(scannedTargetId);

        if (redirected == null && rawScannedId != null) {
            redirected = ServerFieldGuideManager.getInstance().getRedirects().get(rawScannedId);
        }

        if (redirected != null) {
            if (redirected.equals(entryId) || EntryResolver.getRawId(redirected).equals(rawEntryId)) {
                return true;
            }
        }

        return ServerFieldGuideManager.getInstance().isTargetInEntry(scannedTargetId, entryId);
    }
}

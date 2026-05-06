package com.evandev.fieldguide.server.scan;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ScanVerifier {

    public static boolean verifyScan(ServerPlayer player, ResourceLocation entryId,
                                     ResourceLocation scannedTargetId, BlockPos targetBlockPos, int targetEntityId) {
        ServerConfig config = ServerConfig.get();

        if (config.disableScanning) return false;

        boolean hasSpyglass = player.isScoping() || Services.PLATFORM.hasSpyglass(player);

        double activeScanDist;
        if (hasSpyglass && config.enableSpyglassScanning) {
            activeScanDist = config.spyglassScanDistance;
        } else if (config.enableNakedEyeScanning) {
            activeScanDist = config.nakedEyeScanDistance;
        } else {
            return false;
        }

        double maxDistSq = activeScanDist * activeScanDist;
        ServerLevel level = player.serverLevel();

        ResourceLocation categoryId = ServerFieldGuideManager.getInstance().getCategoryForEntryId(entryId);
        if (categoryId == null) {
            return false;
        }

        if (targetEntityId != 0) {
            Entity entity = level.getEntity(targetEntityId);

            if (Services.PLATFORM.isModLoaded("cobblemon") && FieldGuideCobblemonCompat.isPokemon(entity)) {
                if (entity.isSpectator() || player.distanceToSqr(entity) > maxDistSq) {
                    return false;
                }

                ResourceLocation pokemonEntryId = FieldGuideCobblemonCompat.getPokemonEntryId(entity);
                if (targetBelongsToEntry(pokemonEntryId, entryId)) return true;

                return FieldGuideCobblemonCompat.getSpeciesName(pokemonEntryId).equals(FieldGuideCobblemonCompat.getSpeciesName(entryId));
            }

            if (!verifyEntityPresence(player, scannedTargetId, targetEntityId, level, maxDistSq, categoryId)) {
                return false;
            }
        } else if (targetBlockPos != null) {
            if (!verifyBlockPresence(player, scannedTargetId, targetBlockPos, level, maxDistSq, categoryId))
                return false;
        } else {
            return false;
        }

        boolean belongs = targetBelongsToEntry(scannedTargetId, entryId);
        if (!belongs) {
            Constants.LOG.warn("verifyScan failed: targetBelongsToEntry returned false. scannedTargetId: {}, entryId: {}", scannedTargetId, entryId);
        }

        return belongs;
    }

    private static boolean verifyEntityPresence(ServerPlayer player, ResourceLocation scannedTargetId,
                                                int entityId, ServerLevel level, double maxDistSq, ResourceLocation categoryId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null || entity.isSpectator()) return false;
        if (player.distanceToSqr(entity) > maxDistSq) return false;

        if (entity instanceof ItemEntity itemEntity) {
            Item item = itemEntity.getItem().getItem();
            if (!EntryResolver.isValidItem(item, categoryId)) return false;
            ResourceLocation actualItemId = EntryResolver.getEntryId(item, true);
            return actualItemId.equals(scannedTargetId);
        }

        if (!EntryResolver.isValidEntity(entity.getType(), categoryId)) return false;

        ResourceLocation actualTypeId = EntryResolver.getEntryId(entity.getType(), true);
        return actualTypeId.equals(scannedTargetId);
    }

    private static boolean verifyBlockPresence(ServerPlayer player, ResourceLocation scannedTargetId,
                                               BlockPos blockPos, ServerLevel level, double maxDistSq, ResourceLocation categoryId) {
        if (!level.isLoaded(blockPos)) {
            Constants.LOG.warn("verifyBlockPresence failed: Block at {} is not loaded.", blockPos);
            return false;
        }

        double distSq = player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        if (distSq > (maxDistSq + 16.0D)) {
            Constants.LOG.warn("verifyBlockPresence failed: Distance too far. Client max allowed: {}, Server calculated: {}", maxDistSq, distSq);
            return false;
        }

        Block block = level.getBlockState(blockPos).getBlock();
        if (!EntryResolver.isValidBlock(block, categoryId)) {
            Constants.LOG.warn("verifyBlockPresence failed: Block {} is blacklisted or invalid for category {}.", block, categoryId);
            return false;
        }

        ResourceLocation actualBlockId = EntryResolver.getEntryId(block, true);
        ResourceLocation rawBlockId = EntryResolver.getEntryId(block, false);

        boolean idMatches = actualBlockId.equals(scannedTargetId)
                || rawBlockId.equals(scannedTargetId);

        if (!idMatches) {
            Constants.LOG.warn("verifyBlockPresence failed: Block ID mismatch. Server block: {}, Client scanned target: {}", actualBlockId, scannedTargetId);
        }

        return idMatches;
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

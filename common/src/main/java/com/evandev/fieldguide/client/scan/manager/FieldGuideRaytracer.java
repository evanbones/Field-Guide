package com.evandev.fieldguide.client.scan.manager;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.util.ScanRayTraceUtil;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.*;

public class FieldGuideRaytracer {
    private static final FieldGuideRaytracer INSTANCE = new FieldGuideRaytracer();

    private FieldGuideRaytracer() {
    }

    public static FieldGuideRaytracer getInstance() {
        return INSTANCE;
    }

    public void processScanning(Minecraft minecraft) {
        double range = 256.0D;
        if (minecraft.player == null) return;
        Vec3 eyePos = minecraft.player.getEyePosition(1.0F);
        Vec3 viewVec = minecraft.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(range));

        AABB searchBox = minecraft.player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = ScanRayTraceUtil.getScanEntityHitResult(
                minecraft.player, eyePos, endPos, searchBox,
                (entity) -> !entity.isSpectator() && entity.isPickable(),
                range * range
        );

        BlockHitResult blockHit = Objects.requireNonNull(minecraft.level).clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
        BlockHitResult firstBlockHit = blockHit;

        while (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
            if (state.canBeReplaced()) {
                Vec3 hitVec = blockHit.getLocation();
                Vec3 nextStart = hitVec.add(viewVec.scale(0.01));
                if (eyePos.distanceToSqr(nextStart) >= range * range) break;
                blockHit = minecraft.level.clip(new ClipContext(nextStart, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
            } else {
                break;
            }
        }

        Object foundTarget = null;
        Object entryForTarget = null;
        double entityDist = entityHit != null ? eyePos.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;
        double blockDist = blockHit.getType() != HitResult.Type.MISS ? eyePos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double hitDistSq = Math.min(entityDist, blockDist);

        if (entityHit != null && entityDist < blockDist) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof EnderDragonPart part) hitEntity = part.parentMob;

            EntityType<?> type = hitEntity.getType();
            ResourceLocation originalId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            ResourceLocation redirectId = ClientFieldGuideManager.getInstance().getRedirect(originalId);

            Object actualTargetKey = type;
            if (hitEntity instanceof ItemEntity itemEntity) {
                actualTargetKey = itemEntity.getItem().getItem();
            } else if (Services.PLATFORM.isModLoaded("cobblemon") && FieldGuideCobblemonCompat.isPokemon(hitEntity)) {
                actualTargetKey = FieldGuideCobblemonCompat.getPokemonEntryId(hitEntity);
            } else if (redirectId != null) {
                Object redirected = EntryResolver.resolveRegistryObject(redirectId, EntryResolver.RegistryType.ENTITY, EntryResolver.RegistryType.BLOCK);
                if (redirected != null) actualTargetKey = redirected;
            }

            entryForTarget = getContextAwareEntry(actualTargetKey, minecraft, hitEntity.blockPosition());

            if (!(hitEntity instanceof ItemEntity)) {
                List<Object> entries = ClientFieldGuideManager.getInstance().getEntriesForTarget(actualTargetKey);
                for (Object e : entries) {
                    ResourceLocation entryId = ClientFieldGuideManager.getEntryId(e);
                    if (entryId != null && entryId.equals(originalId)) {
                        String key = AutoPopulateRegistry.getEntryKey(e);
                        if (key.startsWith("entity/")) {
                            entryForTarget = e;
                            break;
                        }
                    }
                }
            }

            Category cat = ClientFieldGuideManager.getInstance().getCategoryForEntry(entryForTarget);
            boolean isScannable = cat != null;

            TagKey<EntityType<?>> killToUnlockTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(Constants.MOD_ID, "kill_to_unlock"));
            boolean requiresAction = false;
            if (actualTargetKey instanceof EntityType<?> actualType) {
                var key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(actualType);
                if (key.isPresent()) {
                    var holder = BuiltInRegistries.ENTITY_TYPE.getHolder(key.get());
                    if (holder.isPresent() && holder.get().is(killToUnlockTag)) requiresAction = true;
                }
            } else if (actualTargetKey instanceof net.minecraft.world.item.Item item) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (ClientFieldGuideManager.getInstance().isEatToUnlock(id)) {
                    requiresAction = true;
                }
            }

            if (entryForTarget != null && isScannable && !requiresAction) {
                boolean needsScan = !ProgressManager.getInstance().isUnlocked(entryForTarget);
                if (hitEntity instanceof Mob mob && !ServerConfig.get().unlockAllVariants) {
                    String variantId = FieldGuideVariantManager.getTrackedVariantId(mob);
                    if (!variantId.isEmpty() && !ClientFieldGuideManager.isVariantUnlocked(entryForTarget, variantId)) {
                        needsScan = true;
                    }
                }
                if (needsScan) foundTarget = hitEntity;
            }
        } else {
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
                Block block = state.getBlock();

                ResourceLocation originalId = BuiltInRegistries.BLOCK.getKey(block);
                ResourceLocation redirectId = ClientFieldGuideManager.getInstance().getRedirect(originalId);

                Object actualTargetKey = block;
                if (redirectId != null) {
                    Object redirected = EntryResolver.resolveRegistryObject(redirectId, EntryResolver.RegistryType.BLOCK, EntryResolver.RegistryType.ENTITY);
                    if (redirected != null) actualTargetKey = redirected;
                }

                entryForTarget = getContextAwareEntry(actualTargetKey, minecraft, blockHit.getBlockPos());
                Category cat = ClientFieldGuideManager.getInstance().getCategoryForEntry(entryForTarget);
                if (entryForTarget != null && !ProgressManager.getInstance().isUnlocked(entryForTarget) && cat != null) {
                    foundTarget = block;
                }
            }

            if (foundTarget == null && firstBlockHit.getType() == HitResult.Type.BLOCK && !firstBlockHit.getBlockPos().equals(blockHit.getBlockPos())) {
                BlockState state = minecraft.level.getBlockState(firstBlockHit.getBlockPos());
                Block block = state.getBlock();

                ResourceLocation originalId = BuiltInRegistries.BLOCK.getKey(block);
                ResourceLocation redirectId = ClientFieldGuideManager.getInstance().getRedirect(originalId);

                Object actualTargetKey = block;
                if (redirectId != null) {
                    Object redirected = EntryResolver.resolveRegistryObject(redirectId, EntryResolver.RegistryType.BLOCK, EntryResolver.RegistryType.ENTITY);
                    if (redirected != null) actualTargetKey = redirected;
                }

                entryForTarget = getContextAwareEntry(actualTargetKey, minecraft, firstBlockHit.getBlockPos());
                Category cat = ClientFieldGuideManager.getInstance().getCategoryForEntry(entryForTarget);
                if (entryForTarget != null && !ProgressManager.getInstance().isUnlocked(entryForTarget) && cat != null) {
                    foundTarget = block;
                    blockHit = firstBlockHit;
                    hitDistSq = eyePos.distanceToSqr(firstBlockHit.getLocation());
                }
            }
        }

        FieldGuideScanManager.getInstance().handleTargetAcquisition(minecraft, foundTarget, entryForTarget, hitDistSq, blockHit);
    }

    public Object getContextAwareEntry(Object target, Minecraft minecraft, BlockPos pos) {
        List<Object> possibleEntries = ClientFieldGuideManager.getInstance().getEntriesForTarget(target);
        if (possibleEntries.isEmpty()) return null;

        FieldGuideScanState state = FieldGuideScanState.getInstance();
        if (pos != null && pos.equals(state.getLastDisambiguatedPos())) {
            return state.getLastDisambiguatedEntry();
        }

        Object result = disambiguateComposite(minecraft, pos, possibleEntries, target);
        state.setLastDisambiguatedPos(pos);
        state.setLastDisambiguatedEntry(result);
        return result;
    }

    public Object disambiguateComposite(Minecraft minecraft, BlockPos hitPos, List<Object> possibleEntries, Object actualTargetKey) {
        if (minecraft.level == null || hitPos == null) return possibleEntries.get(0);

        Object bestMatch = null;
        int maxScore = 0;

        int radius = 4;
        Map<Object, Integer> scoreMap = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = hitPos.offset(dx, dy, dz);
                    BlockState state = minecraft.level.getBlockState(pos);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

                    if (state.getBlock().equals(actualTargetKey)) continue;

                    for (Object entry : possibleEntries) {
                        if (entry instanceof GuideEntry composite && composite.isComposite()) {
                            if (composite.childEntries() != null && composite.childEntries().contains(blockId)) {
                                scoreMap.put(entry, scoreMap.getOrDefault(entry, 0) + 1);
                            } else if (composite.displayId() != null && composite.displayId().equals(blockId)) {
                                scoreMap.put(entry, scoreMap.getOrDefault(entry, 0) + 2);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<Object, Integer> entryScore : scoreMap.entrySet()) {
            if (entryScore.getValue() > maxScore) {
                maxScore = entryScore.getValue();
                bestMatch = entryScore.getKey();
            }
        }

        ResourceLocation targetId = actualTargetKey instanceof Block b ? BuiltInRegistries.BLOCK.getKey(b) :
                actualTargetKey instanceof EntityType t ? BuiltInRegistries.ENTITY_TYPE.getKey(t) : null;

        if (maxScore == 0) {
            for (Object entry : possibleEntries) {
                if (entry.equals(actualTargetKey)) return entry;

                ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
                if (Objects.equals(entryId, actualTargetKey)) return entry;

                if (entry instanceof GuideEntry composite && composite.displayId() != null && composite.displayId().equals(targetId)) {
                    return entry;
                }
            }
            return actualTargetKey;
        }

        return bestMatch != null ? bestMatch : possibleEntries.get(0);
    }
}

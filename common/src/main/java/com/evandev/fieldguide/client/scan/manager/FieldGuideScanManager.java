package com.evandev.fieldguide.client.scan.manager;

import com.evandev.fieldguide.ModTags;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.ScanUnlockPacket;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;

public class FieldGuideScanManager {
    public static final int FADE_DURATION = 10;
    private static final FieldGuideScanManager INSTANCE = new FieldGuideScanManager();

    private FieldGuideScanManager() {
    }

    public static FieldGuideScanManager getInstance() {
        return INSTANCE;
    }

    public void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return;

        FieldGuideScanState state = FieldGuideScanState.getInstance();

        boolean hasSpyglass = isUsingSpyglass(minecraft.player);
        boolean canScan = (hasSpyglass && ServerConfig.get().enableSpyglassScanning) || ServerConfig.get().enableNakedEyeScanning;
        boolean isScanningActive = canScan && !ServerConfig.get().disableScanning;

        if (FieldGuideClient.SCAN_KEY != null && !FieldGuideClient.SCAN_KEY.isUnbound()) {
            isScanningActive = isScanningActive && FieldGuideClient.SCAN_KEY.isDown();
        }

        if (isScanningActive) {
            FieldGuideRaytracer.getInstance().processScanning(minecraft);
        } else {
            state.resetScanState();
        }

        if (state.getFadeTicks() > 0) {
            state.setFadeTicks(state.getFadeTicks() - 1);
            if (state.getFadeTicks() <= 0) {
                state.setFadingTarget(null);
                state.setFadingPos(null);
            }
        }
    }

    private boolean isUsingSpyglass(Player player) {
        return player.isScoping() || (player.isUsingItem() && player.getUseItem().is(ModTags.Items.SPYGLASSES));
    }

    public void handleTargetAcquisition(Minecraft minecraft, Object foundTarget, Object resolvedEntry, double hitDistSq, BlockHitResult blockHit) {
        FieldGuideScanState state = FieldGuideScanState.getInstance();
        if (foundTarget == null) {
            state.setOutOfRangeTarget(null);
            state.setOutOfRangePos(null);
            if (state.getScanTicks() > 0) {
                state.setPrevScanTicks(state.getScanTicks());
                state.setScanTicks(state.getScanTicks() - 2);
                if (state.getScanTicks() <= 0) state.resetScanTicks();
            } else {
                state.resetScanTicks();
            }
            return;
        }

        double activeScanDist = (minecraft.player != null && isUsingSpyglass(minecraft.player) && ServerConfig.get().enableSpyglassScanning)
                ? ServerConfig.get().spyglassScanDistance
                : (ServerConfig.get().enableNakedEyeScanning ? ServerConfig.get().nakedEyeScanDistance : 0);

        if (hitDistSq > (activeScanDist * activeScanDist)) {
            state.setOutOfRangeTarget(foundTarget);
            state.setOutOfRangePos((foundTarget instanceof Block) ? blockHit.getBlockPos() : null);
            state.resetScanTicks();
            return;
        }

        state.setOutOfRangeTarget(null);
        state.setOutOfRangePos(null);

        Object targetKey = resolvedEntry;
        if (targetKey == null) {
            BlockPos posContext = (foundTarget instanceof Block) ? blockHit.getBlockPos() : ((Entity) foundTarget).blockPosition();
            Object baseTarget = foundTarget;
            if (foundTarget instanceof Entity entity) {
                baseTarget = (Services.PLATFORM.isModLoaded("cobblemon") && FieldGuideCobblemonCompat.isPokemon(entity))
                        ? FieldGuideCobblemonCompat.getPokemonEntryId(entity)
                        : entity.getType();
            }
            targetKey = FieldGuideRaytracer.getInstance().getContextAwareEntry(baseTarget, minecraft, posContext);
            if (targetKey == null) targetKey = baseTarget;
        }

        ResourceLocation entryId = ClientFieldGuideManager.getEntryId(targetKey);

        if (entryId != null && !ProgressManager.getInstance().canScanToUnlock(entryId)) {
            state.setOutOfRangeTarget(null);
            state.setOutOfRangePos(null);

            if (state.getScanTicks() > 0) {
                state.setPrevScanTicks(state.getScanTicks());
                state.setScanTicks(state.getScanTicks() - 2);
                if (state.getScanTicks() <= 0) state.resetScanTicks();
            } else {
                state.resetScanTicks();
            }
            return;
        }

        boolean sameTarget = (state.getScanningTarget() instanceof Entity && foundTarget instanceof Entity)
                ? state.getScanningTarget() == foundTarget
                : Objects.equals(state.getScanningTarget(), foundTarget);

        if (sameTarget) {
            state.setPrevScanTicks(state.getScanTicks());
            state.setScanTicks(state.getScanTicks() + 1);

            if (foundTarget instanceof Block) state.setScanningPos(blockHit.getBlockPos());

            if (state.getScanTicks() >= (int) (ServerConfig.get().scanSpeed * 20)) {
                completeScan(minecraft, targetKey, foundTarget);
            }
        } else {
            state.setPrevScanTicks(0);
            state.setScanningTarget(foundTarget);
            state.setScanningEntry(targetKey);
            state.setScanningPos((state.getScanningTarget() instanceof Block) ? blockHit.getBlockPos() : null);

            if (ClientConfig.get().playScanningSound) {
                Objects.requireNonNull(minecraft.player).playSound(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 0.4F, 1.0F);
            }
            state.setScanTicks(0);
        }

        if (state.getScanningTarget() instanceof Entity ent && (ent.isRemoved() || !ent.isAlive())) {
            state.resetScanTicks();
        }
    }

    public void completeScan(Minecraft minecraft, Object targetKey, Object foundTarget) {
        FieldGuideScanState state = FieldGuideScanState.getInstance();
        ResourceLocation targetId = ClientFieldGuideManager.getEntryId(targetKey);
        if (targetId != null) {
            ResourceLocation redirectId = ClientFieldGuideManager.getInstance().getRedirect(targetId);
            if (redirectId != null) {
                Object newTargetKey = EntryResolver.resolveRegistryObject(redirectId, EntryResolver.RegistryType.ENTITY, EntryResolver.RegistryType.ITEM, EntryResolver.RegistryType.BLOCK);

                if (newTargetKey != null) {
                    Object resolvedTarget = ClientFieldGuideManager.getInstance().getEntryForTarget(newTargetKey);
                    targetKey = Objects.requireNonNullElse(resolvedTarget, newTargetKey);
                }
            }
        }

        if (ClientConfig.get().playUnlockSound) {
            Objects.requireNonNull(minecraft.player).playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, 1.0F);
        }

        ResourceLocation entryId = ClientFieldGuideManager.getEntryId(targetKey);
        if (entryId != null) {
            ResourceLocation scannedTargetId;
            String variantId = "";

            if (foundTarget instanceof Entity entity) {
                if (entity instanceof ItemEntity itemEntity) {
                    scannedTargetId = ClientFieldGuideManager.getEntryId(itemEntity.getItem().getItem());
                } else {
                    scannedTargetId = ClientFieldGuideManager.getEntryId(entity.getType());
                    if (entity instanceof Mob mob) {
                        variantId = FieldGuideVariantManager.getTrackedVariantId(mob);
                        if (!variantId.isEmpty()) {
                            ProgressManager.getInstance().setSelectedVariant(targetKey, variantId);
                        }
                    }
                }
            } else if (foundTarget instanceof Block block) {
                scannedTargetId = ClientFieldGuideManager.getEntryId(block);
            } else {
                scannedTargetId = ClientFieldGuideManager.getEntryId(foundTarget);
            }
            BlockPos targetBlockPos = (foundTarget instanceof Block) ? state.getScanningPos() : null;
            int targetEntityId = (foundTarget instanceof Entity) ? ((Entity) foundTarget).getId() : 0;

            Services.NETWORK.sendToServer(new ScanUnlockPacket(entryId, variantId, scannedTargetId, targetBlockPos, targetEntityId));
        }

        state.setFadingTarget(foundTarget);
        state.setFadingEntry(targetKey);
        state.setFadingPos((foundTarget instanceof Block) ? state.getScanningPos() : null);
        state.setFadeTicks(FADE_DURATION);

        state.resetScanTicks();
    }

    public float getScanProgress(float partialTicks) {
        FieldGuideScanState state = FieldGuideScanState.getInstance();
        float lerped = (float) state.getPrevScanTicks() + ((float) state.getScanTicks() - (float) state.getPrevScanTicks()) * partialTicks;
        return Math.min(1.0F, lerped / (int) (ServerConfig.get().scanSpeed * 20));
    }

    public float getFadeProgress(float partialTicks) {
        FieldGuideScanState state = FieldGuideScanState.getInstance();
        float currentFade = Math.max(0, state.getFadeTicks() - partialTicks);
        return currentFade / (float) FADE_DURATION;
    }
}
package com.evandev.fieldguide;

import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ModConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.item.ModItems;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class CommonClass {

    public static void init() {
        ModConfig.load();
        ModDataComponents.init();
        ModItems.init();
        FieldGuideTriggers.init();
        FieldGuideCobblemonCompat.init();
    }

    public static void onServerStarted(MinecraftServer server) {
        ServerFieldGuideManager.getInstance().onServerStarted(server);
        FieldGuideProgressManager.init(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        ServerFieldGuideManager.getInstance().syncToPlayer(player);
        FieldGuideProgressManager.getInstance().onPlayerJoin(player);
    }

    public static void onEntityKilled(LivingEntity killed, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();

        if (manager.wasRecentlyScanned(player, killed.getId())) {
            FieldGuideTriggers.SCAN_AND_KILL.get().trigger(player, killed);
        }

        ResourceLocation entityId = EntryResolver.getEntryId(killed.getType());
        PlayerFieldGuideProgress progress = manager.getProgress(player);

        if (progress != null) {
            String variantId = null;
            if (killed instanceof Mob mob) {
                String tracked = FieldGuideVariantManager.getTrackedVariantId(mob);
                if (!tracked.isEmpty()) variantId = tracked;
            }
            progress.tryUnlock(player, entityId, variantId, EntryUnlockData.UnlockTrigger.KILL);
        }
    }
}

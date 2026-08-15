package com.evandev.fieldguide.compat.kubejs;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FieldGuideKubeJSHooks {
    public static void postEntryUnlocked(ServerPlayer player, ResourceLocation entryId, @Nullable String variantId, boolean newlyUnlocked, PlayerFieldGuideProgress progress) {
        if (!FieldGuideEvents.ENTRY_UNLOCKED.hasListeners()) {
            return;
        }

        Set<ResourceLocation> registeredTargets = FieldGuideEvents.ENTRY_UNLOCKED.findUniqueExtraIds(ScriptType.SERVER);
        ResourceLocation target = pickTarget(EntryResolver.getAliasIds(entryId), registeredTargets, entryId, "entryUnlocked");

        FieldGuideEvents.ENTRY_UNLOCKED.post(new EntryUnlockedKubeEvent(player, entryId, variantId, newlyUnlocked, progress), target);
    }

    public static void postCategoryCompleted(ServerPlayer player, ResourceLocation categoryId, PlayerFieldGuideProgress progress) {
        if (!FieldGuideEvents.CATEGORY_COMPLETED.hasListeners()) {
            return;
        }

        Set<ResourceLocation> registeredTargets = FieldGuideEvents.CATEGORY_COMPLETED.findUniqueExtraIds(ScriptType.SERVER);
        List<ResourceLocation> aliases = List.of(categoryId, ResourceLocation.fromNamespaceAndPath("minecraft", categoryId.getPath()));
        ResourceLocation target = pickTarget(aliases, registeredTargets, categoryId, "categoryCompleted");

        FieldGuideEvents.CATEGORY_COMPLETED.post(new CategoryCompletedKubeEvent(player, categoryId, progress), target);
    }

    private static ResourceLocation pickTarget(Iterable<ResourceLocation> aliases, Set<ResourceLocation> registeredTargets, ResourceLocation fallback, String eventName) {
        List<ResourceLocation> matched = new ArrayList<>(2);
        for (ResourceLocation alias : aliases) {
            if (alias != null && registeredTargets.contains(alias)) matched.add(alias);
        }

        if (matched.isEmpty()) return fallback;
        if (matched.size() > 1) {
            Constants.LOG.warn("FieldGuideEvents.{} has listeners registered under several ids for the same target ({}); only '{}' will receive the event. Use one id per target.",
                    eventName, matched, matched.getFirst());
        }
        return matched.getFirst();
    }
}

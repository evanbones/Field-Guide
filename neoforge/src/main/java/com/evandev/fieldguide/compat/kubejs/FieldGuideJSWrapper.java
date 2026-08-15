package com.evandev.fieldguide.compat.kubejs;

import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FieldGuideJSWrapper {
    public ServerFieldGuideManager getManager() {
        return ServerFieldGuideManager.getInstance();
    }

    public FieldGuideProgressManager getProgressManager() {
        return FieldGuideProgressManager.getInstance();
    }

    public PlayerFieldGuideProgress getProgress(ServerPlayer player) {
        return FieldGuideProgressManager.getInstance().getProgress(player);
    }

    public ResourceLocation getCanonicalEntryId(String entryId) {
        try {
            ResourceLocation id = ResourceLocation.parse(entryId);
            return ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(id);
        } catch (Exception e) {
            return null;
        }
    }

    public ResourceLocation getRawEntryId(String entryId) {
        try {
            ResourceLocation id = ResourceLocation.parse(entryId);
            return com.evandev.fieldguide.entry.EntryResolver.getRawId(id);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isUnlocked(ServerPlayer player, String entryId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        return progress != null && progress.isUnlocked(entryId);
    }

    public boolean isUnlocked(ServerPlayer player, String entryId, String variantId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        if (progress == null) return false;
        if (variantId != null && !variantId.isEmpty()) {
            return progress.isUnlocked(entryId, variantId);
        }
        return progress.isUnlocked(entryId);
    }

    public void unlock(ServerPlayer player, String entryId) {
        unlock(player, entryId, null, true);
    }

    public void unlock(ServerPlayer player, String entryId, String variantId) {
        unlock(player, entryId, variantId, true);
    }

    public void unlock(ServerPlayer player, String entryId, String variantId, boolean grantXp) {
        PlayerFieldGuideProgress progress = getProgress(player);
        if (progress != null) {
            try {
                ResourceLocation id = ResourceLocation.parse(entryId);
                progress.unlock(player, id, variantId, grantXp);
            } catch (Exception ignored) {}
        }
    }

    public boolean revoke(ServerPlayer player, String entryId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        return progress != null && progress.revoke(entryId);
    }

    public void revokeAll(ServerPlayer player) {
        PlayerFieldGuideProgress progress = getProgress(player);
        if (progress != null) {
            progress.revokeAll();
        }
    }

    public int getUnlockedCount(ServerPlayer player) {
        PlayerFieldGuideProgress progress = getProgress(player);
        return progress != null ? progress.getUnlockedEntryCount() : 0;
    }

    public Set<String> getUnlockedEntries(ServerPlayer player) {
        PlayerFieldGuideProgress progress = getProgress(player);
        return progress != null ? progress.getUnlockedEntries() : Collections.emptySet();
    }

    public List<String> getUnlockedVariants(ServerPlayer player, String entryId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        return progress != null ? progress.getUnlockedVariants(entryId) : Collections.emptyList();
    }

    public boolean isCategoryCompleted(ServerPlayer player, String categoryId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        if (progress == null) return false;
        try {
            ResourceLocation catId = ResourceLocation.parse(categoryId);
            Set<ResourceLocation> entries = ServerFieldGuideManager.getInstance().getEntryIdsForCategory(catId);
            return !entries.isEmpty() && entries.stream().allMatch(e -> progress.isUnlocked(e.toString()));
        } catch (Exception e) {
            return false;
        }
    }

    public int getUnlockedCountForCategory(ServerPlayer player, String categoryId) {
        PlayerFieldGuideProgress progress = getProgress(player);
        if (progress == null) return 0;
        try {
            ResourceLocation catId = ResourceLocation.parse(categoryId);
            Set<ResourceLocation> entries = ServerFieldGuideManager.getInstance().getEntryIdsForCategory(catId);
            return (int) entries.stream().filter(e -> progress.isUnlocked(e.toString())).count();
        } catch (Exception e) {
            return 0;
        }
    }

    public int getTotalCountForCategory(String categoryId) {
        try {
            ResourceLocation catId = ResourceLocation.parse(categoryId);
            return ServerFieldGuideManager.getInstance().getEntryIdsForCategory(catId).size();
        } catch (Exception e) {
            return 0;
        }
    }
}

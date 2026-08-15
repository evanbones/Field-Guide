package com.evandev.fieldguide.compat.kubejs;

import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

@Info("""
    Invoked when a player unlocks a Field Guide entry or variant.
    """)
public class EntryUnlockedKubeEvent implements KubePlayerEvent {
    private final ServerPlayer player;
    private final ResourceLocation entryId;
    @Nullable
    private final String variantId;
    private final boolean newUnlock;
    private final PlayerFieldGuideProgress progress;

    public EntryUnlockedKubeEvent(ServerPlayer player, ResourceLocation entryId, @Nullable String variantId, boolean newUnlock, PlayerFieldGuideProgress progress) {
        this.player = player;
        this.entryId = entryId;
        this.variantId = variantId;
        this.newUnlock = newUnlock;
        this.progress = progress;
    }

    @Override
    @Info("Returns the player who unlocked the entry.")
    public ServerPlayer getEntity() {
        return player;
    }

    @Info("Returns the ResourceLocation of the unlocked entry.")
    public ResourceLocation getEntryId() {
        return entryId;
    }

    @Info("Returns the entry ID as a string.")
    public String getEntryIdString() {
        return entryId.toString();
    }

    @Info("Returns the raw unprefixed ResourceLocation of the unlocked entry (e.g. minecraft:cow).")
    public ResourceLocation getRawEntryId() {
        return com.evandev.fieldguide.entry.EntryResolver.getRawId(entryId);
    }

    @Info("Returns the raw entry ID as a string (e.g. 'minecraft:cow').")
    public String getRawEntryIdString() {
        ResourceLocation raw = getRawEntryId();
        return raw != null ? raw.toString() : entryId.toString();
    }

    @Info("Returns the variant ID if a variant was unlocked, or empty string if none.")
    public String getVariantId() {
        return variantId != null ? variantId : "";
    }

    @Info("Returns true if this unlock was for a specific variant.")
    public boolean hasVariant() {
        return variantId != null && !variantId.isEmpty();
    }

    @Info("Returns true if this was the first time the player unlocked this entry.")
    public boolean isNewUnlock() {
        return newUnlock;
    }

    @Info("Returns the PlayerFieldGuideProgress object for the player.")
    public PlayerFieldGuideProgress getProgress() {
        return progress;
    }

    @Info("Returns the total number of entries unlocked by the player, not counting variants.")
    public int getUnlockedCount() {
        return progress != null ? progress.getUnlockedEntryCount() : 0;
    }

    @Nullable
    @Info("Returns the ResourceLocation of the category this entry belongs to, or null if unassigned.")
    public ResourceLocation getCategoryId() {
        return ServerFieldGuideManager.getInstance().getCategoryForEntryId(entryId);
    }

    @Nullable
    @Info("Alias for getCategoryId().")
    public ResourceLocation getCategory() {
        return getCategoryId();
    }
}

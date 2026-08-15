package com.evandev.fieldguide.compat.kubejs;

import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

@Info("""
    Invoked when a player completes all entries in a Field Guide category.
    """)
public class CategoryCompletedKubeEvent implements KubePlayerEvent {
    private final ServerPlayer player;
    private final ResourceLocation categoryId;
    private final PlayerFieldGuideProgress progress;

    public CategoryCompletedKubeEvent(ServerPlayer player, ResourceLocation categoryId, PlayerFieldGuideProgress progress) {
        this.player = player;
        this.categoryId = categoryId;
        this.progress = progress;
    }

    @Override
    @Info("Returns the player who completed the category.")
    public ServerPlayer getEntity() {
        return player;
    }

    @Info("Returns the ResourceLocation of the completed category.")
    public ResourceLocation getCategoryId() {
        return categoryId;
    }

    @Info("Alias for getCategoryId().")
    public ResourceLocation getCategory() {
        return categoryId;
    }

    @Info("Returns the category ID as a string.")
    public String getCategoryIdString() {
        return categoryId.toString();
    }

    @Info("Returns the PlayerFieldGuideProgress object for the player.")
    public PlayerFieldGuideProgress getProgress() {
        return progress;
    }
}

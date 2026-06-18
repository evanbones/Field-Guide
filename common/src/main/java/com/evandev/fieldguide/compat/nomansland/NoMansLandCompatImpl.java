package com.evandev.fieldguide.compat.nomansland;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.config.ServerConfig;
import com.farcr.nomansland.client.ClientDialogueTracker;
import com.farcr.nomansland.common.friend.dialogue.DialoguePool;
import com.farcr.nomansland.common.friend.dialogue.DialogueState;
import com.farcr.nomansland.common.registry.NMLRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoMansLandCompatImpl {
    public static final ResourceLocation FIELD_GUIDE_OFFERING_DIALOGUE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "field_guide");

    public static boolean isIntegrationUnlocked() {
        if (ServerConfig.get().enableFieldGuideItem) {
            return ClientDialogueTracker.hasHeardOfferingDialogue(FIELD_GUIDE_OFFERING_DIALOGUE);
        }
        return ClientDialogueTracker.hasHeardAnyDialogue();
    }

    public static boolean hasHeardAnyDialogue() {
        return ClientDialogueTracker.hasHeardAnyDialogue();
    }

    public static List<ResourceLocation> getDialoguesForEntry(Object coreEntry) {
        if (coreEntry instanceof EntityType<?> entityType) {
            return ClientDialogueTracker.getOfferingDialoguesForEntity(entityType);
        }
        Item item = null;
        if (coreEntry instanceof Item directItem) {
            item = directItem;
        } else if (coreEntry instanceof Block block && block.asItem() != Items.AIR) {
            item = block.asItem();
        }
        if (item == null) return List.of();
        return ClientDialogueTracker.getOfferingDialoguesForItem(item);
    }

    public static List<ResourceLocation> getHeardDialogues(List<ResourceLocation> dialogues) {
        List<ResourceLocation> heard = new ArrayList<>();
        for (ResourceLocation dialogue : dialogues) {
            if (ClientDialogueTracker.hasHeardOfferingDialogue(dialogue)) {
                heard.add(dialogue);
            }
        }
        return heard;
    }

    public static void startReplay(ResourceLocation dialogueLocation, String currentDescription) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Optional<Registry<DialoguePool>> registry = minecraft.level.registryAccess().registry(NMLRegistries.OFFERING_DIALOGUE_KEY);
        if (registry.isEmpty()) return;
        DialoguePool pool = registry.get().get(dialogueLocation);
        if (pool == null) return;
        String registryName = NMLRegistries.OFFERING_DIALOGUE_KEY.location().getPath().replace("/", ".");
        FriendMoonReplay.start(new DialogueState(dialogueLocation, registryName, pool), currentDescription);
    }

    public static void stopReplay() {
        FriendMoonReplay.reset();
    }

    public static boolean isReplayActive() {
        return FriendMoonReplay.isActive();
    }

    public static boolean isReplayTyping() {
        return FriendMoonReplay.isTyping();
    }

    public static void renderDescriptionRewrite(GuiGraphics guiGraphics, Font font, int x, int y, int width, int normalColor) {
        FriendMoonReplay.render(guiGraphics, font, x, y, width, normalColor);
    }
}

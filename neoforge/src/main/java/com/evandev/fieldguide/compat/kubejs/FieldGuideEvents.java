package com.evandev.fieldguide.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import net.minecraft.resources.ResourceLocation;

public interface FieldGuideEvents {
    EventGroup GROUP = EventGroup.of("FieldGuideEvents");

    TargetedEventHandler<ResourceLocation> ENTRY_UNLOCKED = GROUP.server("entryUnlocked", () -> EntryUnlockedKubeEvent.class).supportsTarget(EventTargetType.ID);
    TargetedEventHandler<ResourceLocation> CATEGORY_COMPLETED = GROUP.server("categoryCompleted", () -> CategoryCompletedKubeEvent.class).supportsTarget(EventTargetType.ID);
}

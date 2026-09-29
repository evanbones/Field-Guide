package com.evandev.fieldguide.compat.kubejs;

//? if <1.21 {
/*import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

public interface FieldGuideEvents {
    EventGroup GROUP = EventGroup.of("FieldGuideEvents");

    EventHandler ENTRY_UNLOCKED = GROUP.server("entryUnlocked", () -> EntryUnlockedEventJS.class).extra(Extra.ID);
    EventHandler CATEGORY_COMPLETED = GROUP.server("categoryCompleted", () -> CategoryCompletedEventJS.class).extra(Extra.ID);
}
*///?}

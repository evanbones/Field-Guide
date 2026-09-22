package com.evandev.fieldguide;

import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ModConfig;
import com.evandev.fieldguide.item.ModItems;
import com.evandev.fieldguide.server.progress.FieldGuideTriggers;

public class CommonClass {

    public static void init() {
        ModConfig.load();
        ModDataComponents.init();
        ModItems.init();
        FieldGuideTriggers.init();
        FieldGuideCobblemonCompat.init();
    }
}
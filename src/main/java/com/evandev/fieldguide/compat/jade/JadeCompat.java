package com.evandev.fieldguide.compat.jade;

import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.manager.ClientCategoryManager;
import com.evandev.fieldguide.client.scan.manager.FieldGuideRaytracer;
import com.evandev.fieldguide.config.ServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;

//? if <1.21 {
/*import snownee.jade.api.Identifiers;
*///?} else {
import snownee.jade.api.JadeIds;
//?}

//? if >=1.21 <26.1 {
import snownee.jade.api.ui.IBoxElement;
//?} else if >=26.1 {
/*import snownee.jade.api.ui.BoxElement;
*///?}

//? if <26.1 {
import net.minecraft.world.entity.boss.EnderDragonPart;
//?} else {
/*import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
*///?}

class JadeCompat {

    //? if <1.21 {
    /*static void onTooltipCollected(ITooltip tooltip, Accessor<?> accessor) {
    *///?} else if <26.1 {
    static void onTooltipCollected(IBoxElement box, Accessor<?> accessor) {
        ITooltip tooltip = box.getTooltip();
    //?} else {
    /*static void onTooltipCollected(BoxElement box, Accessor<?> accessor) {
        ITooltip tooltip = box.getTooltip();
    *///?}
        if (!ServerConfig.get().hideUndiscoveredInJade || !isLocked(accessor)) return;

        Component hidden = Component.translatable("fieldguide.undiscovered");
        //? if <1.21 {
        /*if (tooltip.get(Identifiers.CORE_OBJECT_NAME).isEmpty()) return;
        tooltip.remove(Identifiers.CORE_OBJECT_NAME);
        tooltip.add(0, hidden, Identifiers.CORE_OBJECT_NAME);
        *///?} else {
        tooltip.replace(JadeIds.CORE_OBJECT_NAME, hidden);
        //?}
    }

    private static boolean isLocked(Accessor<?> accessor) {
        if (accessor instanceof BlockAccessor block) {
            return isLockedTarget(FieldGuideRaytracer.resolveBlockTarget(block.getBlock()));
        }
        if (accessor instanceof EntityAccessor entityAccessor) {
            Entity entity = entityAccessor.getEntity();
            if (entity instanceof EnderDragonPart part) entity = part.parentMob;
            if (entity instanceof ItemEntity) return false;

            if (entity instanceof Mob mob) {
                GuideEntry nbtMatch = ClientCategoryManager.getInstance().findMatchingNbtEntry(mob);
                if (nbtMatch != null) return !ClientFieldGuideManager.isUnlocked(nbtMatch);
            }
            return isLockedTarget(FieldGuideRaytracer.resolveEntityTarget(entity));
        }
        return false;
    }

    private static boolean isLockedTarget(Object target) {
        ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
        boolean tracked = false;
        for (Object entry : manager.getEntriesForTarget(target)) {
            if (manager.getCategoryForEntry(entry) == null) continue;
            if (ClientFieldGuideManager.isUnlocked(entry)) return false;
            tracked = true;
        }
        return tracked;
    }
}

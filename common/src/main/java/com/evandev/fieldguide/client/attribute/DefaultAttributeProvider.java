package com.evandev.fieldguide.client.attribute;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.attribute.AttributeProvider;
import com.evandev.fieldguide.api.attribute.GuideAttribute;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.data.EntryVisual;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefaultAttributeProvider implements AttributeProvider {
    @Override
    public List<GuideAttribute> getAttributes(Object entry, @Nullable Entity renderedEntity) {
        List<GuideAttribute> attributes = new ArrayList<>();

        if (renderedEntity instanceof LivingEntity living) {
            // Alignment
            ResourceLocation icon;
            Component typeComponent;

            if (living instanceof NeutralMob) {
                icon = Constants.NEUTRAL_ICON;
                typeComponent = Component.translatable("fieldguide.alignment.neutral");
            } else if (living.getType().getCategory() == MobCategory.MONSTER) {
                icon = Constants.HOSTILE_ICON;
                typeComponent = Component.translatable("fieldguide.alignment.hostile");
            } else {
                icon = Constants.PASSIVE_ICON;
                typeComponent = Component.translatable("fieldguide.alignment.passive");
            }

            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(entry);
            if (visual != null && visual.alignmentIcon != null) {
                icon = visual.alignmentIcon;
                if (icon.equals(Constants.NEUTRAL_ICON)) {
                    typeComponent = Component.translatable("fieldguide.alignment.neutral");
                } else if (icon.equals(Constants.HOSTILE_ICON)) {
                    typeComponent = Component.translatable("fieldguide.alignment.hostile");
                } else if (icon.equals(Constants.PASSIVE_ICON)) {
                    typeComponent = Component.translatable("fieldguide.alignment.passive");
                }
            }

            attributes.add(GuideAttribute.of(icon, 0, 0, 9, 9, 9, 9, null, typeComponent));

            // Health
            String health = String.valueOf((int) living.getMaxHealth() / 2);
            attributes.add(GuideAttribute.of(Constants.HEALTH_ICON, 0, 0, 9, 9, 9, 9, health, Component.translatable("fieldguide.attribute.health")));

            // Armor
            int armorValue = living.getArmorValue();
            if (armorValue > 0) {
                attributes.add(GuideAttribute.of(Constants.ARMOR_ICON, 0, 0, 9, 9, 9, 9, String.valueOf(armorValue), Component.translatable("fieldguide.attribute.armor")));
            }
        }

        return attributes;
    }
}

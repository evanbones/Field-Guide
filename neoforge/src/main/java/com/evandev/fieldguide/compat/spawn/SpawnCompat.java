package com.evandev.fieldguide.compat.spawn;

import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.ninni.spawn.Spawn;
import com.ninni.spawn.server.data.AnimalVariantManager;
import com.ninni.spawn.server.entity.mob.CoastalCrab;
import com.ninni.spawn.server.entity.mob.Firekeeper;
import com.ninni.spawn.server.entity.mob.Stickbug;
import com.ninni.spawn.server.entity.util.JsonVariantHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpawnCompat {

    public static class SpawnVariantProvider implements VariantProvider<Mob> {

        @Override
        public List<VariantDef> getVariants(Mob entity) {
            if (!(entity instanceof JsonVariantHolder)) return List.of();

            List<VariantDef> variants = new ArrayList<>();
            for (AnimalVariantManager.AnimalVariantData data : Spawn.PROXY.getAnimalVariantManager().DATA) {
                if (data == null || data.hidden()) continue;
                if (data.type().equals(entity.getType()) || (data.childType().isPresent() && data.childType().get().equals(entity.getType()))) {
                    variants.add(new VariantDef(data.id().toString(), data.id()));
                }
            }
            return variants;
        }

        @Override
        public void apply(Mob entity, VariantDef def) {
            if (!(entity instanceof JsonVariantHolder holder)) return;

            if (entity instanceof Stickbug stickbug) stickbug.setItem(ItemStack.EMPTY);
            if (entity instanceof Firekeeper firekeeper) {
                firekeeper.setLanded(true);
                firekeeper.awakeningAnimationState.stop();
                firekeeper.idleAnimationState.start(firekeeper.tickCount);
            }
            if (entity instanceof CoastalCrab crab) {
                crab.setYLookDirection(0.0f);
                crab.setYLookDirectionOld(0.0f);
            }
            if (def.value() instanceof ResourceLocation id) {
                holder.setVariant(id);
                AnimalVariantManager.AnimalVariantData data = AnimalVariantManager.get(id);
                if (data != null) holder.setVariantScale(data.scale());
            }
        }

        @Override
        public VariantDef getCurrent(Mob entity) {
            if (!(entity instanceof JsonVariantHolder holder)) return new VariantDef("default", null);
            ResourceLocation current = holder.getVariant();
            if (current == null) return new VariantDef("default", null);
            return new VariantDef(current.toString(), current);
        }

        @Override
        public boolean suppressesDefaultVariant(Mob entity) {
            return entity instanceof JsonVariantHolder;
        }

        @Override
        public String getCacheKey(Mob entity) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "_spawn";
        }
    }
}

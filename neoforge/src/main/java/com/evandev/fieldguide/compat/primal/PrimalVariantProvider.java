package com.evandev.fieldguide.compat.primal;

import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Mob;
import org.primal.util.mob_types.ReplacedEntityNewVariantHolder;

import java.util.Arrays;
import java.util.List;

public class PrimalVariantProvider implements VariantProvider<Mob> {

    @Override
    public List<VariantDef> getVariants(Mob entity) {
        if (entity instanceof ReplacedEntityNewVariantHolder<?> holder) {
            Object current = holder.primal$getVariant();
            if (current != null && current.getClass().isEnum()) {
                return Arrays.stream(current.getClass().getEnumConstants())
                        .map(e -> {
                            String name = e instanceof StringRepresentable sr ? sr.getSerializedName() : ((Enum<?>) e).name();
                            return new VariantDef(name, e);
                        })
                        .toList();
            }
        }
        return List.of();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void apply(Mob entity, VariantDef def) {
        if (entity instanceof ReplacedEntityNewVariantHolder holder && def.value() != null) {
            try {
                holder.primal$setVariant((StringRepresentable) def.value());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public VariantDef getCurrent(Mob entity) {
        if (entity instanceof ReplacedEntityNewVariantHolder<?> holder) {
            Object current = holder.primal$getVariant();
            if (current != null) {
                String name = current instanceof StringRepresentable sr ? sr.getSerializedName() : current.toString();
                return new VariantDef(name, current);
            }
        }
        return new VariantDef("default", null);
    }

    @Override
    public String getCacheKey(Mob entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "_primal";
    }
}
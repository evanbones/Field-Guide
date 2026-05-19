package com.evandev.fieldguide.compat.mixedlitter;

import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import dev.tazer.mixed_litter.MLRegistries;
import dev.tazer.mixed_litter.VariantUtil;
import dev.tazer.mixed_litter.registry.MLDataAttachmentTypes;
import dev.tazer.mixed_litter.variants.Variant;
import dev.tazer.mixed_litter.variants.VariantGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MixedLitterCompat {

    private static boolean isForEntity(ResourceLocation id, String entityPath) {
        if (id == null) return false;
        String path = id.getPath().toLowerCase();

        if (path.startsWith(entityPath + "/")) {
            return true;
        }

        if (path.equals(entityPath)) {
            return true;
        }

        return path.equals("remodeled_" + entityPath) || path.equals(entityPath + "_variants");
    }

    public static void applyDummyVariant(Entity entity) {
        if (entity instanceof AgeableMob ageable) {
            ageable.setAge(0);
        }
        try {
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            String entityPath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

            List<Variant> selected = new ArrayList<>();
            for (Holder<VariantGroup> groupHolder : groupRegistry.holders().toList()) {
                VariantGroup group = groupHolder.value();
                ResourceLocation groupId = groupRegistry.getKey(group);
                if (groupId == null) continue;

                List<Variant> matching = new ArrayList<>();

                for (ResourceLocation id : variantRegistry.keySet()) {
                    Variant variant = variantRegistry.get(id);
                    if (variant != null && variant.group().isPresent() && variant.group().get().equals(groupId)) {
                        if (isForEntity(id, entityPath) || isForEntity(groupId, entityPath)) {
                            matching.add(variant);
                        }
                    }
                }

                if (!matching.isEmpty()) {
                    matching.sort((v1, v2) -> {
                        boolean c1 = v1.conditions().isPresent();
                        boolean c2 = v2.conditions().isPresent();

                        if (!c1 && c2) return -1;
                        if (c1 && !c2) return 1;

                        ResourceLocation id1 = variantRegistry.getKey(v1);
                        ResourceLocation id2 = variantRegistry.getKey(v2);
                        String p1 = id1 != null ? id1.getPath() : "";
                        String p2 = id2 != null ? id2.getPath() : "";

                        boolean d1 = p1.contains("default") || p1.equals(entityPath);
                        boolean d2 = p2.contains("default") || p2.equals(entityPath);

                        if (d1 && !d2) return -1;
                        if (!d1 && d2) return 1;

                        return p1.compareTo(p2);
                    });

                    selected.add(matching.getFirst());
                }
            }

            for (ResourceLocation id : variantRegistry.keySet()) {
                Variant variant = variantRegistry.get(id);
                if (variant != null && variant.group().isEmpty()) {
                    if (isForEntity(id, entityPath)) {
                        if (variant.conditions().isEmpty()) {
                            selected.add(variant);
                        }
                    }
                }
            }

            if (!selected.isEmpty()) {
                VariantUtil.setVariants(entity, selected);
            }
        } catch (Exception ignored) {
        }
    }

    public static List<VariantDef> getVariants(Entity entity) {
        List<VariantDef> defs = new ArrayList<>();
        try {
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            String entityPath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

            List<ResourceLocation> sortedKeys = new ArrayList<>(variantRegistry.keySet());
            Collections.sort(sortedKeys);

            for (ResourceLocation id : sortedKeys) {
                Variant variant = variantRegistry.get(id);
                if (variant == null) continue;

                boolean matches = isForEntity(id, entityPath);
                if (!matches && variant.group().isPresent()) {
                    matches = isForEntity(variant.group().get(), entityPath);
                }

                if (matches) {
                    defs.add(new VariantDef(id.toString(), id));
                }
            }
        } catch (Exception ignored) {
        }
        return defs;
    }

    public static void applyVariant(Entity entity, VariantDef def) {
        try {
            List<ResourceLocation> currentIds = entity.getData(MLDataAttachmentTypes.VARIANTS.get());
            if (currentIds.isEmpty()) {
                applyDummyVariant(entity);
            }
        } catch (Exception ignored) {
        }

        if (def == null || def.value() == null) {
            try {
                applyDummyVariant(entity);
            } catch (Exception ignored) {
            }
            return;
        }

        switch (def.value()) {
            case ResourceLocation newVariantId -> {
                try {
                    Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
                    Variant newVariant = variantRegistry.get(newVariantId);
                    if (newVariant == null) return;

                    List<ResourceLocation> currentIds = new ArrayList<>(entity.getData(MLDataAttachmentTypes.VARIANTS.get()));

                    if (newVariant.group().isPresent()) {
                        ResourceLocation newGroup = newVariant.group().get();
                        currentIds.removeIf(id -> {
                            Variant v = variantRegistry.get(id);
                            return v != null && v.group().isPresent() && v.group().get().equals(newGroup);
                        });
                    }

                    if (!currentIds.contains(newVariantId)) {
                        currentIds.add(newVariantId);
                    }

                    List<Variant> updatedVariants = new ArrayList<>();
                    for (ResourceLocation id : currentIds) {
                        Variant v = variantRegistry.get(id);
                        if (v != null) {
                            updatedVariants.add(v);
                        }
                    }

                    VariantUtil.setVariants(entity, updatedVariants);
                } catch (Exception ignored) {
                }
            }
            case CompoundTag compoundTag -> {
                try {
                    VariantUtil.setVariants(entity, new ArrayList<>());
                } catch (Exception ignored) {
                }
            }
            default -> {
            }
        }
    }

    public static VariantDef getCurrentVariant(Entity entity) {
        try {
            List<ResourceLocation> variants = entity.getData(MLDataAttachmentTypes.VARIANTS.get());
            if (!variants.isEmpty()) {
                ResourceLocation id = variants.getLast();
                return new VariantDef(id.toString(), id);
            }
        } catch (Exception ignored) {
        }
        return new VariantDef("default", null);
    }

    public static class MixedLitterVariantProvider implements VariantProvider<Mob> {
        @Override
        public List<VariantDef> getVariants(Mob entity) {
            return MixedLitterCompat.getVariants(entity);
        }

        @Override
        public void apply(Mob mob, VariantDef def) {
            MixedLitterCompat.applyVariant(mob, def);
        }

        @Override
        public VariantDef getCurrent(Mob entity) {
            return MixedLitterCompat.getCurrentVariant(entity);
        }

        @Override
        public String getCacheKey(Mob entity) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id + "_mixed_litter";
        }
    }
}
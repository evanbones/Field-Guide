package com.evandev.fieldguide.compat.mixedlitter;

import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import dev.tazer.mixed_litter.MLRegistries;
import dev.tazer.mixed_litter.VariantUtil;
import dev.tazer.mixed_litter.client.RemodelRegistry;
import dev.tazer.mixed_litter.registry.MLDataAttachmentTypes;
import dev.tazer.mixed_litter.variants.EntityConditions;
import dev.tazer.mixed_litter.variants.Variant;
import dev.tazer.mixed_litter.variants.VariantGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MixedLitterCompat {

    private static boolean isForEntity(Variant variant, ResourceLocation variantId, ResourceLocation entityId) {
        if (variantId == null || entityId == null) return false;

        String variantNs = variantId.getNamespace();
        if (!variantNs.equals("minecraft") && !variantNs.equals("mixed_litter")) {
            String entityNs = entityId.getNamespace();
            if (!entityNs.equals("minecraft") && !entityNs.equals(variantNs)) {
                return false;
            }
        }

        String entityPath = entityId.getPath().toLowerCase();
        String variantPath = variantId.getPath().toLowerCase();

        int slashIdx = variantPath.indexOf('/');
        if (slashIdx > 0) {
            return variantPath.substring(0, slashIdx).equals(entityPath);
        }

        if (variant.group().isPresent() && variant.group().get().getPath().toLowerCase().contains(entityPath)) {
            return true;
        }

        return variant.arguments() != null && variant.arguments().toString().toLowerCase().contains(entityPath);
    }

    private static boolean isAntlerVariant(Variant variant) {
        return variant.type().getPath().contains("antler");
    }

    /**
     * Whether ML's remodel (and therefore its texture variants) is currently active for this entity.
     * Players can disable a remodel via ML's startup config, in which case {@link RemodelRegistry}
     * stops reporting a remodel for the entity and the vanilla appearance is used.
     */
    private static boolean isRemodelActive(Entity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return RemodelRegistry.remodelFor(entityId.toString()) != null;
    }

    private static boolean conditionsAllow(Optional<EntityConditions> conditions, boolean remodelActive) {
        if (conditions.isEmpty()) return true;
        EntityConditions ec = conditions.get();
        if (ec.unobtainable()) return false;
        return ec.remodel().map(remodel -> remodel == remodelActive).orElse(true);
    }

    /**
     * Mirrors ML's own gating: a variant only renders when its own and its group's {@code remodel}
     * condition matches the active remodel state, and it isn't flagged unobtainable. This keeps the
     * field guide in sync with whatever the player has enabled in ML's config.
     */
    private static boolean isVariantEnabled(Variant variant, Registry<VariantGroup> groupRegistry, boolean remodelActive) {
        if (!conditionsAllow(variant.conditions(), remodelActive)) return false;
        if (variant.group().isPresent()) {
            VariantGroup group = groupRegistry.get(variant.group().get());
            return group == null || conditionsAllow(group.conditions(), remodelActive);
        }
        return true;
    }

    public static boolean hasReplaceDefaultGroup(Entity entity) {
        try {
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean remodelActive = isRemodelActive(entity);
            for (ResourceLocation id : variantRegistry.keySet()) {
                Variant variant = variantRegistry.get(id);
                if (variant == null || !isForEntity(variant, id, entityId)) continue;
                if (variant.group().isEmpty()) continue;
                if (!isVariantEnabled(variant, groupRegistry, remodelActive)) continue;
                VariantGroup group = groupRegistry.get(variant.group().get());
                if (group != null && group.replaceDefault()) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Returns true if ML has full visual-replacement variants for this entity (e.g. texture
     * swaps like rabbit/brown).  Returns false for model-only variants like the sheep remodel,
     * which complement the vanilla DyeColor system instead of replacing it.
     */
    private static boolean hasFullVariants(Entity entity) {
        try {
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean remodelActive = isRemodelActive(entity);
            for (ResourceLocation id : variantRegistry.keySet()) {
                Variant variant = variantRegistry.get(id);
                if (variant == null) continue;
                if (!isForEntity(variant, id, entityId)) continue;
                if (!isVariantEnabled(variant, groupRegistry, remodelActive)) continue;
                String ns = variant.type().getNamespace();
                String path = variant.type().getPath();
                if (ns.equals("mixed_litter") && (path.equals("sheep") || path.equals("simple"))) continue;
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void applyDummyVariant(Entity entity) {
        if (entity instanceof AgeableMob ageable) {
            ageable.setAge(0);
        }
        try {
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean remodelActive = isRemodelActive(entity);

            List<Variant> selected = new ArrayList<>();

            for (Holder<VariantGroup> groupHolder : groupRegistry.holders().toList()) {
                ResourceLocation groupId = groupRegistry.getKey(groupHolder.value());
                List<Variant> matching = new ArrayList<>();

                for (ResourceLocation id : variantRegistry.keySet()) {
                    Variant variant = variantRegistry.get(id);
                    if (variant != null && variant.group().isPresent() && variant.group().get().equals(groupId)) {
                        if (isForEntity(variant, id, entityId) && isVariantEnabled(variant, groupRegistry, remodelActive)) {
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
                        return 0;
                    });
                    selected.add(matching.getFirst());
                }
            }

            for (ResourceLocation id : variantRegistry.keySet()) {
                Variant variant = variantRegistry.get(id);
                if (variant != null && variant.group().isEmpty() && isForEntity(variant, id, entityId)
                        && isVariantEnabled(variant, groupRegistry, remodelActive)) {
                    if (variant.conditions().isEmpty()) {
                        selected.add(variant);
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
        if (!hasFullVariants(entity)) return List.of();

        List<VariantDef> defs = new ArrayList<>();
        try {
            Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean remodelActive = isRemodelActive(entity);

            List<ResourceLocation> sortedKeys = new ArrayList<>(variantRegistry.keySet());
            Collections.sort(sortedKeys);

            for (ResourceLocation id : sortedKeys) {
                Variant variant = variantRegistry.get(id);
                if (variant == null) continue;
                if (!isForEntity(variant, id, entityId)) continue;
                if (!isVariantEnabled(variant, groupRegistry, remodelActive)) continue;
                if (isAntlerVariant(variant)) continue;
                defs.add(new VariantDef(id.toString(), id));
            }
        } catch (Exception ignored) {
        }
        return defs;
    }

    public static void applyVariant(Entity entity, VariantDef def) {
        if (def == null || def.value() == null) {
            try {
                VariantUtil.setVariants(entity, new ArrayList<>());
                entity.removeData(MLDataAttachmentTypes.VARIANTS.get());
            } catch (Exception ignored) {
            }
            return;
        }
        if (!(def.value() instanceof ResourceLocation newVariantId)) {
            if (hasFullVariants(entity)) {
                try {
                    VariantUtil.setVariants(entity, new ArrayList<>());
                    entity.removeData(MLDataAttachmentTypes.VARIANTS.get());
                } catch (Exception ignored) {
                }
            } else {
                applyDummyVariant(entity);
            }
            return;
        }

        try {
            Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
            Variant newVariant = variantRegistry.get(newVariantId);
            if (newVariant == null) return;

            List<ResourceLocation> currentIds = entity.hasData(MLDataAttachmentTypes.VARIANTS) ?
                    new ArrayList<>(entity.getData(MLDataAttachmentTypes.VARIANTS)) : new ArrayList<>();

            if (currentIds.isEmpty()) {
                applyDummyVariant(entity);
                currentIds = entity.hasData(MLDataAttachmentTypes.VARIANTS) ?
                        new ArrayList<>(entity.getData(MLDataAttachmentTypes.VARIANTS)) : new ArrayList<>();
            }

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

    public static VariantDef getCurrentVariant(Entity entity) {
        try {
            if (entity.hasData(MLDataAttachmentTypes.VARIANTS)) {
                List<ResourceLocation> variantIds = entity.getData(MLDataAttachmentTypes.VARIANTS);
                if (!variantIds.isEmpty()) {
                    Registry<Variant> variantRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_KEY);
                    Registry<VariantGroup> groupRegistry = entity.registryAccess().registryOrThrow(MLRegistries.VARIANT_GROUP_KEY);
                    for (ResourceLocation id : variantIds) {
                        Variant variant = variantRegistry.get(id);
                        if (variant == null || variant.group().isEmpty()) continue;
                        VariantGroup group = groupRegistry.get(variant.group().get());
                        if (group != null && group.replaceDefault()) {
                            return new VariantDef(id.toString(), id);
                        }
                    }
                    return new VariantDef(variantIds.getLast().toString(), variantIds.getLast());
                }
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
        public boolean suppressesDefaultVariant(Mob entity) {
            return MixedLitterCompat.hasReplaceDefaultGroup(entity);
        }

        @Override
        public String getCacheKey(Mob entity) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id + "_mixed_litter";
        }
    }
}
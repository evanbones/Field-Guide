package com.evandev.fieldguide.entry;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.*;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntryResolutionHelper {

    public static List<Object> resolveCategoryEntries(Category category, Map<ResourceLocation, GuideEntry> allEntries, List<CompositeDefinition> globalComposites, Map<ResourceLocation, ResourceLocation> redirects) {
        Set<Object> foundEntries = new LinkedHashSet<>();
        Set<String> addedKeys = new HashSet<>();
        ResourceLocation categoryId = category.getId();

        for (ResourceLocation entryId : category.getEntryIds()) {
            GuideEntry entry = allEntries.get(entryId);
            if (entry == null) continue;

            if (entry.isAutoPopulate() && entry.strategy() != null) {
                for (Object obj : AutoPopulateRegistry.getEntries(entry.strategy(), categoryId)) {
                    String key = AutoPopulateRegistry.getEntryKey(obj);
                    if (!key.isEmpty() && !addedKeys.contains(key)) {
                        foundEntries.add(obj);
                        addedKeys.add(key);
                    }
                }
            } else if (entry.isVirtual()) {
                foundEntries.add(entry);
                addedKeys.add(entry.id().toString());
            } else {
                ResourceLocation targetId = entry.displayId() != null ? entry.displayId() : entry.id();
                resolveSingleEntry(targetId, categoryId, entry.strategy()).ifPresent(e -> {
                    foundEntries.add(entry.isComposite() || entry.isStructure() || entry.targetEntityType() != null ? entry : e);
                    addedKeys.add(AutoPopulateRegistry.getEntryKey(e));
                });
            }
        }

        if (globalComposites != null && !globalComposites.isEmpty()) {
            List<Object> groupedEntries = new ArrayList<>();
            Set<ResourceLocation> processedComposites = new HashSet<>();

            for (Object raw : foundEntries) {
                ResourceLocation baseId = getEntryId(raw);
                if (raw instanceof GuideEntry ge) {
                    baseId = ge.displayId() != null ? ge.displayId() : ge.id();
                }

                CompositeDefinition matchingDef = findCompositeFor(baseId, globalComposites);

                if (matchingDef != null) {
                    if (processedComposites.add(matchingDef.id())) {
                        resolveCompositeDefinition(matchingDef, categoryId, raw).ifPresent(groupedEntries::add);
                    }
                } else {
                    groupedEntries.add(raw);
                }
            }
            foundEntries.clear();
            foundEntries.addAll(groupedEntries);
        }

        return getResolvedEntries(redirects, foundEntries);
    }

    private static @NotNull List<Object> getResolvedEntries(Map<ResourceLocation, ResourceLocation> redirects, Set<Object> foundEntries) {
        List<Object> resolved = new ArrayList<>(foundEntries);
        resolved.removeIf(e -> {
            ResourceLocation id = getEntryId(e);
            if (id != null && redirects.containsKey(id)) {
                ResourceLocation targetId = redirects.get(id);
                if (targetId != null) {
                    ResourceLocation rawTargetId = EntryResolver.getRawId(targetId);
                    return BuiltInRegistries.ITEM.containsKey(rawTargetId) ||
                            BuiltInRegistries.BLOCK.containsKey(rawTargetId) ||
                            BuiltInRegistries.ENTITY_TYPE.containsKey(rawTargetId);
                }
            }
            return false;
        });
        return resolved;
    }

    private static CompositeDefinition findCompositeFor(ResourceLocation id, List<CompositeDefinition> composites) {
        if (id == null) return null;
        for (CompositeDefinition def : composites) {
            ResourceLocation mainId = def.displayId() != null ? def.displayId() : def.id();
            if (id.equals(mainId) || (def.components() != null && def.components().contains(id))) {
                return def;
            }
        }
        return null;
    }

    public static Optional<Object> resolveSingleEntryWithHint(ResourceLocation id, ResourceLocation categoryId, Object hint) {
        ResourceLocation rawId = EntryResolver.getRawId(id);
        if (hint instanceof EntityType<?>) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(rawId)
                    .filter(t -> EntryValidator.isValidEntity(t, categoryId))
                    .map(Object.class::cast)
                    .or(() -> resolveSingleEntry(id, categoryId, "animals"));
        } else if (hint instanceof Item) {
            return BuiltInRegistries.ITEM.getOptional(rawId)
                    .filter(i -> EntryValidator.isValidItem(i, categoryId))
                    .map(Object.class::cast)
                    .or(() -> resolveSingleEntry(id, categoryId, "mod_items"));
        } else if (hint instanceof Block) {
            return BuiltInRegistries.BLOCK.getOptional(rawId)
                    .filter(b -> EntryValidator.isValidBlock(b, categoryId))
                    .map(Object.class::cast)
                    .or(() -> resolveSingleEntry(id, categoryId, "plants"));
        }
        return resolveSingleEntry(id, categoryId, null);
    }

    public static Optional<GuideEntry> resolveCompositeDefinition(CompositeDefinition def, ResourceLocation categoryId, Object hint) {
        ResourceLocation displayLoc = def.displayId() != null ? def.displayId() : def.id();
        return resolveSingleEntryWithHint(displayLoc, categoryId, hint).map(displayEntry -> {
            List<ResourceLocation> components = new ArrayList<>();
            if (def.components() != null) {
                for (ResourceLocation compId : def.components()) {
                    resolveSingleEntryWithHint(compId, categoryId, hint).ifPresent(resolved -> components.add(compId));
                }
            }

            boolean hasStructure = def.structureNbt() != null || (def.stackedBlocks() != null && !def.stackedBlocks().isEmpty());
            StructureData structureData = hasStructure ? new StructureData(def.structureNbt(), def.stackedBlocks()) : null;

            boolean hasVisualVariants = def.visualVariants() != null && !def.visualVariants().isEmpty();
            EntryKind kind = (hasStructure || hasVisualVariants) ? EntryKind.STRUCTURE : EntryKind.NORMAL;

            return new GuideEntry(def.id(), displayLoc, null, kind, false, false, null, components, structureData, def.visualVariants(), null, EntryUnlockData.DEFAULT, null, null);
        });
    }

    public static Optional<Object> resolveSingleEntry(ResourceLocation id, ResourceLocation categoryId, String strategyHint) {
        if (Services.PLATFORM.isModLoaded("cobblemon") && id.getNamespace().equals(Constants.MOD_ID) && id.getPath().startsWith("cobblemon/")) {
            return Optional.of(new GuideEntry(id, null, null, EntryKind.NORMAL, true, false, null, null, null, null, new VirtualData("cobblemon"), EntryUnlockData.DEFAULT, null, null));
        }

        ResourceLocation finalId = EntryResolver.getRawId(id);
        String namespace = id.getNamespace();

        switch (namespace) {
            case "item" -> {
                return lookupItem(finalId, categoryId);
            }
            case "entity" -> {
                return lookupEntity(finalId, categoryId);
            }
            case "block" -> {
                return lookupBlock(finalId, categoryId);
            }
        }

        String effectiveStrategy = getEffectiveStrategy(categoryId, strategyHint);

        boolean preferEntity = effectiveStrategy != null && (effectiveStrategy.equals("animals") || effectiveStrategy.equals("monsters") || effectiveStrategy.startsWith("mod_entities"));
        boolean preferItem = effectiveStrategy != null && (effectiveStrategy.startsWith("mod_items"));
        boolean preferBlock = effectiveStrategy != null && (effectiveStrategy.equals("plants") || effectiveStrategy.equals("trees") || effectiveStrategy.startsWith("mod_plants") || effectiveStrategy.startsWith("mod_blocks"));

        if (preferEntity) {
            Optional<Object> entity = lookupEntity(finalId, categoryId);
            if (entity.isPresent()) return entity;
        }
        if (preferBlock) {
            Optional<Object> block = lookupBlock(finalId, categoryId);
            if (block.isPresent()) return block;
        }
        if (preferItem) {
            Optional<Object> item = lookupItem(finalId, categoryId);
            if (item.isPresent()) return item;
        }

        return lookupBlock(finalId, categoryId)
                .or(() -> lookupItem(finalId, categoryId))
                .or(() -> lookupEntity(finalId, categoryId));
    }

    private static Optional<Object> lookupEntity(ResourceLocation id, ResourceLocation categoryId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .filter(t -> EntryValidator.isValidEntity(t, categoryId))
                .map(Object.class::cast);
    }

    private static Optional<Object> lookupBlock(ResourceLocation id, ResourceLocation categoryId) {
        return BuiltInRegistries.BLOCK.getOptional(id)
                .filter(b -> EntryValidator.isValidBlock(b, categoryId))
                .map(Object.class::cast);
    }

    private static Optional<Object> lookupItem(ResourceLocation id, ResourceLocation categoryId) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .filter(i -> EntryValidator.isValidItem(i, categoryId))
                .map(Object.class::cast);
    }

    private static @Nullable String getEffectiveStrategy(ResourceLocation categoryId, String strategyHint) {
        String effectiveStrategy = strategyHint;
        if (effectiveStrategy == null && categoryId != null) {
            String path = categoryId.getPath();
            if (path.contains("animal") || path.contains("monster") || path.contains("entity"))
                effectiveStrategy = "animals";
            else if (path.contains("item")) effectiveStrategy = "mod_items";
            else if (path.contains("plant") || path.contains("tree") || path.contains("block"))
                effectiveStrategy = "plants";
        }
        return effectiveStrategy;
    }

    public static ResourceLocation getEntryId(Object obj) {
        if (obj instanceof GuideEntry ge) return ge.id();
        return AutoPopulateRegistry.getEntryId(obj);
    }
}
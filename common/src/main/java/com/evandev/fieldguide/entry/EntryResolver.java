package com.evandev.fieldguide.entry;

import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.GuideEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class EntryResolver {

    public static boolean hasEntry(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation entryId, @Nullable ResourceLocation canonicalId) {
        if (entryId == null) return false;
        ResourceLocation rawId = getRawId(entryId);
        for (List<Object> entries : resolvedEntries.values()) {
            for (Object entry : entries) {
                ResourceLocation id = getEntryId(entry, true);
                if (entryId.equals(id) || (canonicalId != null && canonicalId.equals(id)) || (rawId != null && rawId.equals(getRawId(id)))) return true;
                if (entry instanceof GuideEntry ge) {
                    if (entryId.equals(ge.id()) || (canonicalId != null && canonicalId.equals(ge.id())) || (rawId != null && rawId.equals(getRawId(ge.id())))) return true;
                }
            }
        }
        return false;
    }

    public static ResourceLocation getCategoryForEntryId(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation entryId, @Nullable ResourceLocation canonicalId) {
        if (entryId == null) return null;
        ResourceLocation rawId = getRawId(entryId);
        for (Map.Entry<ResourceLocation, List<Object>> cat : resolvedEntries.entrySet()) {
            for (Object entry : cat.getValue()) {
                ResourceLocation id = getEntryId(entry, true);
                if (entryId.equals(id) || (canonicalId != null && canonicalId.equals(id)) || (rawId != null && rawId.equals(getRawId(id)))) return cat.getKey();
                if (entry instanceof GuideEntry ge) {
                    if (entryId.equals(ge.id()) || (canonicalId != null && canonicalId.equals(ge.id())) || (rawId != null && rawId.equals(getRawId(ge.id())))) return cat.getKey();
                }
            }
        }
        return null;
    }

    public static Set<ResourceLocation> getAllEntryIds(Map<ResourceLocation, List<Object>> resolvedEntries) {
        return resolvedEntries.values().stream()
                .flatMap(List::stream)
                .map(EntryResolver::getEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Set<ResourceLocation> getEntryIdsForCategory(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation categoryId) {
        List<Object> entries = resolvedEntries.get(categoryId);
        if (entries == null) return Collections.emptySet();
        return entries.stream()
                .map(EntryResolver::getEntryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static ResourceLocation resolveCanonicalEntryId(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation id) {
        ResourceLocation canonical = findCanonicalEntryId(resolvedEntries, id);
        return canonical != null ? canonical : id;
    }

    @Nullable
    public static ResourceLocation findCanonicalEntryId(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation id) {
        if (id == null) return null;

        for (List<Object> entries : resolvedEntries.values()) {
            for (Object entry : entries) {
                ResourceLocation entryId = getEntryId(entry, true);
                if (id.equals(entryId)) return entryId;
                if (entry instanceof GuideEntry ge && id.equals(ge.id())) return ge.id();
            }
        }

        String path = id.getPath();
        if (path.startsWith("entity/") || path.startsWith("item/") || path.startsWith("block/")) {
            int slashIdx = path.indexOf('/');
            String typePrefix = path.substring(0, slashIdx);
            String rawPath = path.substring(slashIdx + 1);
            ResourceLocation converted = ResourceLocation.fromNamespaceAndPath(typePrefix, id.getNamespace() + "/" + rawPath);
            for (List<Object> entries : resolvedEntries.values()) {
                for (Object entry : entries) {
                    ResourceLocation entryId = getEntryId(entry, true);
                    if (converted.equals(entryId)) return entryId;
                    if (entry instanceof GuideEntry ge && converted.equals(ge.id())) return ge.id();
                }
            }
        }

        ResourceLocation rawId = getRawId(id);
        if (rawId != null) {
            String prefix = rawId.getNamespace() + "/" + rawId.getPath();
            ResourceLocation entityPrefix = ResourceLocation.fromNamespaceAndPath("entity", prefix);
            ResourceLocation blockPrefix = ResourceLocation.fromNamespaceAndPath("block", prefix);
            ResourceLocation itemPrefix = ResourceLocation.fromNamespaceAndPath("item", prefix);

            boolean isEntity = id.getNamespace().equals("entity") || id.getPath().startsWith("entity/");
            boolean isBlock = id.getNamespace().equals("block") || id.getPath().startsWith("block/");
            boolean isItem = id.getNamespace().equals("item") || id.getPath().startsWith("item/");

            List<ResourceLocation> candidates = new ArrayList<>();
            if (isEntity) candidates.add(entityPrefix);
            else if (isBlock) candidates.add(blockPrefix);
            else if (isItem) candidates.add(itemPrefix);

            candidates.add(entityPrefix);
            candidates.add(blockPrefix);
            candidates.add(itemPrefix);

            for (ResourceLocation candidate : candidates) {
                for (List<Object> entries : resolvedEntries.values()) {
                    for (Object entry : entries) {
                        ResourceLocation entryId = getEntryId(entry, true);
                        if (candidate.equals(entryId)) return entryId;
                        if (entry instanceof GuideEntry ge && candidate.equals(ge.id())) return ge.id();
                    }
                }
            }

            for (List<Object> entries : resolvedEntries.values()) {
                for (Object entry : entries) {
                    ResourceLocation entryId = getEntryId(entry, true);
                    if (rawId.equals(getRawId(entryId))) return entryId;
                    if (entry instanceof GuideEntry ge) {
                        if (rawId.equals(getRawId(ge.displayId()))) return ge.id();
                        if (rawId.equals(getRawId(ge.id()))) return ge.id();
                    }
                }
            }
        }

        return null;
    }

    public static Object getEntryForTarget(Map<ResourceLocation, List<Object>> resolvedEntries, Object target) {
        List<Object> entries = getEntriesForTarget(resolvedEntries, target);
        if (entries.isEmpty()) return null;

        ResourceLocation rawTargetId = target instanceof ResourceLocation loc ? getRawId(loc) : getEntryId(target, false);

        for (Object entry : entries) {
            if (entry.equals(target)) return entry;
            if (entry instanceof GuideEntry guideEntry && guideEntry.isComposite()) {
                if (guideEntry.displayId() != null && guideEntry.displayId().equals(rawTargetId)) {
                    return entry;
                }
            }
        }

        return entries.getFirst();
    }

    public static List<Object> getEntriesForTarget(Map<ResourceLocation, List<Object>> resolvedEntries, Object target) {
        List<Object> matches = new ArrayList<>();
        ResourceLocation prefixedTargetId = target instanceof ResourceLocation loc ? loc : getEntryId(target, true);
        ResourceLocation rawTargetId = target instanceof ResourceLocation loc ? getRawId(loc) : getEntryId(target, false);

        for (List<Object> entries : resolvedEntries.values()) {
            for (Object entry : entries) {
                if (entry.equals(target) || prefixedTargetId.equals(getEntryId(entry, true))) {
                    matches.add(entry);
                } else if (entry instanceof GuideEntry guideEntry && guideEntry.isComposite()) {
                    if ((guideEntry.displayId() != null && guideEntry.displayId().equals(rawTargetId)) ||
                            (guideEntry.childEntries() != null && guideEntry.childEntries().contains(rawTargetId))) {
                        matches.add(entry);
                    }
                }
            }
        }
        return matches;
    }

    public static boolean isTargetInEntry(Map<ResourceLocation, List<Object>> resolvedEntries, ResourceLocation targetId, ResourceLocation entryId) {
        ResourceLocation rawTargetId = getRawId(targetId);

        for (List<Object> entries : resolvedEntries.values()) {
            for (Object entry : entries) {
                ResourceLocation id = getEntryId(entry, true);
                if (!entryId.equals(id)) continue;

                if (targetId.equals(id) || rawTargetId.equals(id)) return true;

                if (entry instanceof GuideEntry guideEntry && guideEntry.isComposite()) {
                    if (rawTargetId.equals(guideEntry.displayId())) return true;
                    if (guideEntry.childEntries() != null && guideEntry.childEntries().contains(rawTargetId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isValidEntity(EntityType<?> type, ResourceLocation categoryId) {
        return EntryValidator.isValidEntity(type, categoryId);
    }

    public static boolean isValidBlock(Block block, ResourceLocation categoryId) {
        return EntryValidator.isValidBlock(block, categoryId);
    }

    public static boolean isValidItem(Item item, ResourceLocation categoryId) {
        return EntryValidator.isValidItem(item, categoryId);
    }

    public static ResourceLocation getEntryId(Object obj) {
        return getEntryId(obj, true);
    }

    public static ResourceLocation getEntryId(Object obj, boolean prefixed) {
        return AutoPopulateRegistry.getEntryId(obj, prefixed);
    }

    public static ResourceLocation getRawId(ResourceLocation id) {
        if (id == null) return null;
        String ns = id.getNamespace();
        if (ns.equals("item") || ns.equals("entity") || ns.equals("block")) {
            return ResourceLocation.parse(id.getPath().replaceFirst("/", ":"));
        }
        String path = id.getPath();
        if (path.startsWith("entity/") || path.startsWith("item/") || path.startsWith("block/")) {
            String strippedPath = path.substring(path.indexOf('/') + 1);
            return ResourceLocation.fromNamespaceAndPath(ns, strippedPath);
        }
        return id;
    }

    public static boolean isUnlocked(Set<String> unlockedEntries, ResourceLocation id, @Nullable ResourceLocation canonicalId, @Nullable String variantId) {
        if (id == null || unlockedEntries == null || unlockedEntries.isEmpty()) return false;

        String idStr = id.toString();
        String suffix = (variantId != null && !variantId.isEmpty()) ? "#" + variantId : "";

        if (unlockedEntries.contains(idStr + suffix)) return true;

        if (canonicalId != null && unlockedEntries.contains(canonicalId + suffix)) {
            return true;
        }

        ResourceLocation rawId = getRawId(id);
        if (rawId != null) {
            String rawStr = rawId.toString();
            if (unlockedEntries.contains(rawStr + suffix)) return true;

            for (String u : unlockedEntries) {
                if (variantId != null && !variantId.isEmpty()) {
                    int hashIdx = u.indexOf('#');
                    if (hashIdx != -1 && u.substring(hashIdx + 1).equals(variantId)) {
                        if (rawIdsCompatible(u.substring(0, hashIdx), id)) return true;
                    }
                } else {
                    if (!u.contains("#") && rawIdsCompatible(u, id)) return true;
                }
            }
        }
        return false;
    }

    public static boolean rawIdsCompatible(String storedId, ResourceLocation queryId) {
        try {
            return rawIdsCompatible(ResourceLocation.parse(storedId), queryId);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean rawIdsCompatible(ResourceLocation storedId, ResourceLocation queryId) {
        ResourceLocation storedRaw = getRawId(storedId);
        if (storedRaw == null || !storedRaw.equals(getRawId(queryId))) return false;

        RegistryType storedType = explicitType(storedId);
        RegistryType queryType = explicitType(queryId);
        return storedType == null || queryType == null || storedType == queryType;
    }

    public static boolean matchesStoredEntry(String storedKey, String queryKey, @Nullable ResourceLocation canonicalLoc, @Nullable ResourceLocation queryLoc) {
        if (storedKey == null || queryKey == null) return false;
        if (storedKey.equals(queryKey) || storedKey.startsWith(queryKey + "#")) return true;

        if (canonicalLoc != null) {
            String canonicalStr = canonicalLoc.toString();
            if (storedKey.equals(canonicalStr) || storedKey.startsWith(canonicalStr + "#")) return true;
        }

        if (queryLoc != null) {
            ResourceLocation rawLoc = getRawId(queryLoc);
            if (rawLoc != null) {
                String rawStr = rawLoc.toString();
                if (storedKey.equals(rawStr) || storedKey.startsWith(rawStr + "#")) return true;
            }

            int hashIdx = storedKey.indexOf('#');
            String base = hashIdx != -1 ? storedKey.substring(0, hashIdx) : storedKey;
            if (rawIdsCompatible(base, queryLoc)) return true;
        }

        return false;
    }

    public static ResourceLocation getPathPrefixedId(ResourceLocation id) {
        if (id == null) return null;
        String ns = id.getNamespace();
        if (ns.equals("entity") || ns.equals("item") || ns.equals("block")) {
            ResourceLocation rawId = getRawId(id);
            if (rawId != null) {
                return ResourceLocation.fromNamespaceAndPath(rawId.getNamespace(), ns + "/" + rawId.getPath());
            }
        }
        return id;
    }

    public static ResourceLocation getNamespacePrefixedId(ResourceLocation id) {
        if (id == null) return null;
        RegistryType type = explicitType(id);
        if (type == null) return id;
        ResourceLocation rawId = getRawId(id);
        if (rawId == null) return id;
        return ResourceLocation.fromNamespaceAndPath(type.prefix, rawId.getNamespace() + "/" + rawId.getPath());
    }

    public static Set<ResourceLocation> getAliasIds(ResourceLocation id) {
        if (id == null) return Collections.emptySet();
        Set<ResourceLocation> aliases = new LinkedHashSet<>();
        aliases.add(id);
        if (explicitType(id) != null) {
            aliases.add(getNamespacePrefixedId(id));
            aliases.add(getPathPrefixedId(id));
        }
        ResourceLocation rawId = getRawId(id);
        if (rawId != null) aliases.add(rawId);
        return aliases;
    }

    public enum RegistryType {
        ENTITY("entity"),
        BLOCK("block"),
        ITEM("item");

        private final String prefix;

        RegistryType(String prefix) {
            this.prefix = prefix;
        }

        private Object lookup(ResourceLocation rawId) {
            return switch (this) {
                case ENTITY -> BuiltInRegistries.ENTITY_TYPE.getOptional(rawId).orElse(null);
                case BLOCK -> BuiltInRegistries.BLOCK.getOptional(rawId).orElse(null);
                case ITEM -> BuiltInRegistries.ITEM.getOptional(rawId).orElse(null);
            };
        }
    }

    @Nullable
    public static RegistryType explicitType(ResourceLocation id) {
        if (id == null) return null;
        for (RegistryType type : RegistryType.values()) {
            if (id.getNamespace().equals(type.prefix) || id.getPath().startsWith(type.prefix + "/")) return type;
        }
        return null;
    }

    public static Object resolveRegistryObject(ResourceLocation id) {
        return resolveRegistryObject(id, RegistryType.ENTITY, RegistryType.BLOCK, RegistryType.ITEM);
    }

    public static Object resolveRegistryObject(ResourceLocation id, RegistryType... order) {
        if (id == null) return null;
        ResourceLocation rawId = getRawId(id);
        if (rawId == null) return null;

        RegistryType explicit = explicitType(id);
        if (explicit != null) return explicit.lookup(rawId);

        for (RegistryType type : order) {
            Object obj = type.lookup(rawId);
            if (obj != null) return obj;
        }
        return null;
    }

    public static List<Object> resolveAllRegistryObjects(ResourceLocation id) {
        if (id == null) return Collections.emptyList();
        ResourceLocation rawId = getRawId(id);
        if (rawId == null) return Collections.emptyList();

        RegistryType explicit = explicitType(id);
        if (explicit != null) {
            Object obj = explicit.lookup(rawId);
            return obj != null ? List.of(obj) : Collections.emptyList();
        }

        List<Object> objects = new ArrayList<>(3);
        for (RegistryType type : RegistryType.values()) {
            Object obj = type.lookup(rawId);
            if (obj != null) objects.add(obj);
        }
        return objects;
    }

    public static Object resolveCoreEntry(Object entry) {
        if (entry instanceof GuideEntry ge && ge.displayId() != null) {
            Object obj = resolveRegistryObject(ge.displayId(), RegistryType.BLOCK, RegistryType.ITEM, RegistryType.ENTITY);
            return obj != null ? obj : entry;
        }
        return entry;
    }
}
package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.client.search.SearchManager;
import com.evandev.fieldguide.entry.EntryResolutionHelper;
import com.evandev.fieldguide.entry.EntryResolver;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

public class ClientCategoryManager {
    private static final ClientCategoryManager INSTANCE = new ClientCategoryManager();

    private final Map<ResourceLocation, Category> syncedCategories = new LinkedHashMap<>();
    private final Map<ResourceLocation, GuideEntry> syncedEntries = new HashMap<>();
    private final Map<ResourceLocation, List<Object>> resolvedCategoryEntries = new HashMap<>();
    private final Map<ResourceLocation, ResourceLocation> redirects = new HashMap<>();
    private final List<String> biomeAdditions = new ArrayList<>();
    private final List<String> biomeRemovals = new ArrayList<>();
    private final Map<String, List<ResourceLocation>> indexedBiomeAdditions = new HashMap<>();
    private final Map<String, List<ResourceLocation>> indexedBiomeRemovals = new HashMap<>();
    private final List<String> lootAdditions = new ArrayList<>();
    private final List<String> lootRemovals = new ArrayList<>();
    private final Map<String, List<ResourceLocation>> indexedLootAdditions = new HashMap<>();
    private final Map<String, List<ResourceLocation>> indexedLootRemovals = new HashMap<>();
    private boolean needsResolution = false;

    private ClientCategoryManager() {
    }

    public static ClientCategoryManager getInstance() {
        return INSTANCE;
    }

    public void updateCategoriesFromServer(List<Category> categories, List<GuideEntry> entries, Map<ResourceLocation, ResourceLocation> redirects, boolean clearCache, boolean resolveEntries) {
        if (clearCache) {
            this.redirects.clear();
            this.syncedCategories.clear();
            this.syncedEntries.clear();
        }

        this.redirects.putAll(redirects);

        for (GuideEntry entry : entries) {
            this.syncedEntries.put(entry.id(), entry);
        }

        for (Category cat : categories) {
            if (this.syncedCategories.containsKey(cat.getId())) {
                Category existing = this.syncedCategories.get(cat.getId());
                if (cat.getEntryIds() != null) {
                    for (ResourceLocation entryId : cat.getEntryIds()) {
                        existing.addEntryId(entryId);
                    }
                }
                if (cat.getGroupByQueries() != null && !cat.getGroupByQueries().isEmpty()) {
                    existing.setGroupByQueries(cat.getGroupByQueries());
                }
            } else {
                this.syncedCategories.put(cat.getId(), cat);
            }
        }

        if (resolveEntries) {
            List<Category> sorted = new ArrayList<>(this.syncedCategories.values());
            sorted.sort(Comparator.comparingInt(Category::getSortIndex).thenComparing(Category::getId));
            this.syncedCategories.clear();
            for (Category cat : sorted) this.syncedCategories.put(cat.getId(), cat);

            this.needsResolution = true;
        }
    }

    public void updateModifiers(List<String> biomeAdditions, List<String> biomeRemovals, List<String> lootAdditions, List<String> lootRemovals, boolean clearCache) {
        if (clearCache) {
            this.biomeAdditions.clear();
            this.biomeRemovals.clear();
            this.indexedBiomeAdditions.clear();
            this.indexedBiomeRemovals.clear();
            this.lootAdditions.clear();
            this.lootRemovals.clear();
            this.indexedLootAdditions.clear();
            this.indexedLootRemovals.clear();
        }

        if (!biomeAdditions.isEmpty()) {
            this.biomeAdditions.addAll(biomeAdditions);
            for (String addition : biomeAdditions) {
                indexModifier(this.indexedBiomeAdditions, addition);
            }
        }
        if (!biomeRemovals.isEmpty()) {
            this.biomeRemovals.addAll(biomeRemovals);
            for (String removal : biomeRemovals) {
                indexModifier(this.indexedBiomeRemovals, removal);
            }
        }
        if (!lootAdditions.isEmpty()) {
            this.lootAdditions.addAll(lootAdditions);
            for (String addition : lootAdditions) {
                indexModifier(this.indexedLootAdditions, addition);
            }
        }
        if (!lootRemovals.isEmpty()) {
            this.lootRemovals.addAll(lootRemovals);
            for (String removal : lootRemovals) {
                indexModifier(this.indexedLootRemovals, removal);
            }
        }
    }

    private void indexModifier(Map<String, List<ResourceLocation>> index, String line) {
        String[] parts = line.split("\\|", 2);
        if (parts.length == 2) {
            try {
                ResourceLocation valueLoc = new ResourceLocation(parts[1]);
                String targetKey = parts[0];
                index.computeIfAbsent(targetKey, k -> new ArrayList<>()).add(valueLoc);
                if (targetKey.contains("#")) {
                    int hash = targetKey.indexOf('#');
                    String base = targetKey.substring(0, hash);
                    String var = targetKey.substring(hash + 1);
                    if (base.startsWith("entity:") || base.startsWith("block:") || base.startsWith("item:")) {
                        String rawBase = base.substring(base.indexOf(':') + 1).replace('/', ':');
                        index.computeIfAbsent(rawBase + "#" + var, k -> new ArrayList<>()).add(valueLoc);
                    } else {
                        index.computeIfAbsent("entity:" + base.replace(':', '/') + "#" + var, k -> new ArrayList<>()).add(valueLoc);
                    }
                } else {
                    if (targetKey.startsWith("entity:") || targetKey.startsWith("block:") || targetKey.startsWith("item:")) {
                        String rawBase = targetKey.substring(targetKey.indexOf(':') + 1).replace('/', ':');
                        index.computeIfAbsent(rawBase, k -> new ArrayList<>()).add(valueLoc);
                    } else {
                        index.computeIfAbsent("entity:" + targetKey.replace(':', '/'), k -> new ArrayList<>()).add(valueLoc);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public void resolveAllEntries() {
        resolvedCategoryEntries.clear();
        syncedCategories.values().forEach(category -> {
            List<Object> entries = EntryResolutionHelper.resolveCategoryEntries(category, syncedEntries, Collections.emptyList(), this.redirects);
            List<Object> groupedEntries = SearchManager.groupByQueries(entries, category.getGroupByQueries());

            resolvedCategoryEntries.put(category.getId(), groupedEntries);
        });
    }

    public Map<ResourceLocation, Category> getCategories() {
        return syncedCategories;
    }

    public GuideEntry getGuideEntry(ResourceLocation id) {
        return syncedEntries.get(id);
    }

    public GuideEntry getGuideEntryForTarget(Object target) {
        if (target instanceof GuideEntry ge) return ge;
        ResourceLocation targetId = AutoPopulateRegistry.getEntryId(target, true);
        return syncedEntries.get(targetId);
    }

    public List<Object> getValidEntries() {
        return resolvedCategoryEntries.values().stream().flatMap(List::stream).distinct().collect(Collectors.toList());
    }

    public List<Object> getEntriesForCategory(Category category) {
        return resolvedCategoryEntries.getOrDefault(category.getId(), Collections.emptyList());
    }

    public List<Object> getRecentEntries(Category category, int limit) {
        return getEntriesForCategory(category).stream()
                .filter(ProgressManager.getInstance()::isUnlocked)
                .sorted((a, b) -> Long.compare(ProgressManager.getInstance().getDiscoveryTime(b), ProgressManager.getInstance().getDiscoveryTime(a)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Category getCategoryForEntry(Object entry) {
        return resolvedCategoryEntries.entrySet().stream().filter(e -> e.getValue().contains(entry)).map(e -> syncedCategories.get(e.getKey())).findFirst().orElse(null);
    }

    public Object getEntryForTarget(Object target) {
        return EntryResolver.getEntryForTarget(resolvedCategoryEntries, target);
    }

    public List<Object> getEntriesForTarget(Object target) {
        return EntryResolver.getEntriesForTarget(resolvedCategoryEntries, target);
    }

    public ResourceLocation getRedirect(ResourceLocation source) {
        if (source == null) return null;
        ResourceLocation target = redirects.get(source);
        if (target != null) return target;
        ResourceLocation raw = EntryResolver.getRawId(source);
        if (raw != null) {
            return redirects.get(raw);
        }
        return null;
    }

    public Map<ResourceLocation, List<Object>> getResolvedCategoryEntries() {
        return resolvedCategoryEntries;
    }

    public boolean isNeedsResolution() {
        return needsResolution;
    }

    public void setNeedsResolution(boolean needsResolution) {
        this.needsResolution = needsResolution;
    }

    public List<String> getBiomeAdditions() {
        return biomeAdditions;
    }

    public List<String> getBiomeRemovals() {
        return biomeRemovals;
    }

    public List<String> getLootAdditions() {
        return lootAdditions;
    }

    private List<String> getLookupKeys(Object entry, String variantId) {
        Set<String> baseKeys = new LinkedHashSet<>();

        ResourceLocation entryId = EntryResolver.getEntryId(entry);
        if (entryId != null) {
            baseKeys.add(entryId.toString());
            ResourceLocation rawId = EntryResolver.getRawId(entryId);
            if (rawId != null) {
                baseKeys.add(rawId.toString());
                baseKeys.add("entity:" + rawId.getNamespace() + "/" + rawId.getPath());
                baseKeys.add("entity:" + rawId.getNamespace() + ":" + rawId.getPath());
            }
        }

        String key = AutoPopulateRegistry.getEntryKey(entry);
        if (!key.isEmpty()) {
            baseKeys.add(key);
            if (key.contains("/")) {
                baseKeys.add(key.replaceFirst("/", ":"));
            }
        }

        ResourceLocation rawLoc = AutoPopulateRegistry.getEntryId(entry, false);
        if (rawLoc != null) {
            baseKeys.add(rawLoc.toString());
            ResourceLocation rawId = EntryResolver.getRawId(rawLoc);
            if (rawId != null) baseKeys.add(rawId.toString());
        }

        List<String> keys = new ArrayList<>();
        if (variantId != null && !variantId.isEmpty()) {
            String variantPath = variantId.contains(":") ? variantId.substring(variantId.indexOf(':') + 1) : variantId;
            for (String base : baseKeys) {
                keys.add(base + "#" + variantId);
                if (!variantPath.equals(variantId)) {
                    keys.add(base + "#" + variantPath);
                }
            }
        }

        keys.addAll(baseKeys);
        return keys;
    }

    public List<ResourceLocation> getBiomeAdditions(Object entry, String variantId) {
        List<ResourceLocation> additions = new ArrayList<>();
        for (String lookupKey : getLookupKeys(entry, variantId)) {
            List<ResourceLocation> list = indexedBiomeAdditions.get(lookupKey);
            if (list != null) additions.addAll(list);
        }
        return additions;
    }

    public List<ResourceLocation> getBiomeRemovals(Object entry, String variantId) {
        List<ResourceLocation> removals = new ArrayList<>();
        for (String lookupKey : getLookupKeys(entry, variantId)) {
            List<ResourceLocation> list = indexedBiomeRemovals.get(lookupKey);
            if (list != null) removals.addAll(list);
        }
        return removals;
    }

    public List<String> getLootRemovals() {
        return lootRemovals;
    }

    public List<ResourceLocation> getLootAdditions(Object entry) {
        return getLootAdditions(entry, null);
    }

    public List<ResourceLocation> getLootAdditions(Object entry, String variantId) {
        List<ResourceLocation> additions = new ArrayList<>();
        for (String lookupKey : getLookupKeys(entry, variantId)) {
            List<ResourceLocation> list = indexedLootAdditions.get(lookupKey);
            if (list != null) additions.addAll(list);
        }
        return additions;
    }

    public List<ResourceLocation> getLootRemovals(Object entry) {
        return getLootRemovals(entry, null);
    }

    public List<ResourceLocation> getLootRemovals(Object entry, String variantId) {
        List<ResourceLocation> removals = new ArrayList<>();
        for (String lookupKey : getLookupKeys(entry, variantId)) {
            List<ResourceLocation> list = indexedLootRemovals.get(lookupKey);
            if (list != null) removals.addAll(list);
        }
        return removals;
    }

}
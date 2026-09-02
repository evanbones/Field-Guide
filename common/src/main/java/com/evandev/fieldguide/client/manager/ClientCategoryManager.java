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
        }

        if (!biomeAdditions.isEmpty()) {
            this.biomeAdditions.addAll(biomeAdditions);
            for (String addition : biomeAdditions) {
                String[] parts = addition.split("\\|", 2);
                if (parts.length == 2) {
                    this.indexedBiomeAdditions.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(new ResourceLocation(parts[1]));
                }
            }
        }
        if (!biomeRemovals.isEmpty()) {
            this.biomeRemovals.addAll(biomeRemovals);
            for (String removal : biomeRemovals) {
                String[] parts = removal.split("\\|", 2);
                if (parts.length == 2) {
                    this.indexedBiomeRemovals.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(new ResourceLocation(parts[1]));
                }
            }
        }
        if (!lootAdditions.isEmpty()) {
            this.lootAdditions.addAll(lootAdditions);
            for (String addition : lootAdditions) {
                String[] parts = addition.split("\\|", 2);
                if (parts.length == 2) {
                    this.indexedLootAdditions.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(new ResourceLocation(parts[1]));
                }
            }
        }
        if (!lootRemovals.isEmpty()) {
            this.lootRemovals.addAll(lootRemovals);
            for (String removal : lootRemovals) {
                String[] parts = removal.split("\\|", 2);
                if (parts.length == 2) {
                    this.indexedLootRemovals.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(new ResourceLocation(parts[1]));
                }
            }
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

    public List<ResourceLocation> getBiomeAdditions(Object entry, String variantId) {
        String key = AutoPopulateRegistry.getEntryKey(entry);
        String baseId = Objects.requireNonNull(AutoPopulateRegistry.getEntryId(entry, false)).toString();
        List<ResourceLocation> additions = new ArrayList<>();

        if (variantId != null && !variantId.isEmpty()) {
            if (indexedBiomeAdditions.containsKey(key + "#" + variantId)) additions.addAll(indexedBiomeAdditions.get(key + "#" + variantId));
            if (indexedBiomeAdditions.containsKey(baseId + "#" + variantId)) additions.addAll(indexedBiomeAdditions.get(baseId + "#" + variantId));
        }

        if (indexedBiomeAdditions.containsKey(key)) additions.addAll(indexedBiomeAdditions.get(key));
        if (indexedBiomeAdditions.containsKey(baseId)) additions.addAll(indexedBiomeAdditions.get(baseId));

        return additions;
    }

    public List<ResourceLocation> getBiomeRemovals(Object entry, String variantId) {
        String key = AutoPopulateRegistry.getEntryKey(entry);
        String baseId = Objects.requireNonNull(AutoPopulateRegistry.getEntryId(entry, false)).toString();
        List<ResourceLocation> removals = new ArrayList<>();

        if (variantId != null && !variantId.isEmpty()) {
            if (indexedBiomeRemovals.containsKey(key + "#" + variantId)) removals.addAll(indexedBiomeRemovals.get(key + "#" + variantId));
            if (indexedBiomeRemovals.containsKey(baseId + "#" + variantId)) removals.addAll(indexedBiomeRemovals.get(baseId + "#" + variantId));
        }

        if (indexedBiomeRemovals.containsKey(key)) removals.addAll(indexedBiomeRemovals.get(key));
        if (indexedBiomeRemovals.containsKey(baseId)) removals.addAll(indexedBiomeRemovals.get(baseId));

        return removals;
    }

    public List<String> getLootRemovals() {
        return lootRemovals;
    }

    public List<ResourceLocation> getLootAdditions(Object entry) {
        String key = AutoPopulateRegistry.getEntryKey(entry);
        String baseId = AutoPopulateRegistry.getEntryId(entry, false).toString();
        List<ResourceLocation> additions = new ArrayList<>();
        if (indexedLootAdditions.containsKey(key)) additions.addAll(indexedLootAdditions.get(key));
        if (indexedLootAdditions.containsKey(baseId)) additions.addAll(indexedLootAdditions.get(baseId));
        return additions;
    }

    public List<ResourceLocation> getLootRemovals(Object entry) {
        String key = AutoPopulateRegistry.getEntryKey(entry);
        String baseId = AutoPopulateRegistry.getEntryId(entry, false).toString();
        List<ResourceLocation> removals = new ArrayList<>();
        if (indexedLootRemovals.containsKey(key)) removals.addAll(indexedLootRemovals.get(key));
        if (indexedLootRemovals.containsKey(baseId)) removals.addAll(indexedLootRemovals.get(baseId));
        return removals;
    }

}
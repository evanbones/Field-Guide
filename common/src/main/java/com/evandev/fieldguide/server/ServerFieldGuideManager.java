package com.evandev.fieldguide.server;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.*;
import com.evandev.fieldguide.api.variant.DatapackVariant;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ModConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolutionHelper;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.ExportContentPacket;
import com.evandev.fieldguide.network.SyncCategoriesPacket;
import com.evandev.fieldguide.network.SyncConfigPacket;
import com.evandev.fieldguide.network.SyncLootPacket;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.loot.LootTableHelper;
import com.evandev.fieldguide.server.loot.StaticLootParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.*;

public class ServerFieldGuideManager extends SimplePreparableReloadListener<ServerFieldGuideManager.ReloadData> {
    private static final ServerFieldGuideManager INSTANCE = new ServerFieldGuideManager();
    private final Map<ResourceLocation, List<Object>> resolvedCategoryEntries = new HashMap<>();
    private final Map<ResourceLocation, EntryUnlockData> entryUnlockDataMap = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> triggerOnMap = new HashMap<>();

    private Map<ResourceLocation, Category> categories = new LinkedHashMap<>();
    private Map<ResourceLocation, GuideEntry> allEntries = new HashMap<>();

    private List<CompositeDefinition> composites = new ArrayList<>();
    private Map<ResourceLocation, List<ItemStack>> serverLootCache = new HashMap<>();
    private Map<ResourceLocation, ResourceLocation> redirects = new HashMap<>();
    private Map<ResourceLocation, List<DatapackVariant>> variants = new HashMap<>();
    private List<String> biomeAdditions = new ArrayList<>();
    private List<String> biomeRemovals = new ArrayList<>();
    private List<String> lootAdditions = new ArrayList<>();
    private List<String> lootRemovals = new ArrayList<>();
    private List<String> prefixedBiomeAdditions = new ArrayList<>();
    private List<String> prefixedLootAdditions = new ArrayList<>();

    public static ServerFieldGuideManager getInstance() {
        return INSTANCE;
    }

    public EntryUnlockData getUnlockData(ResourceLocation entryId) {
        EntryUnlockData mapData = entryUnlockDataMap.get(entryId);
        if (mapData != null && !EntryUnlockData.DEFAULT.equals(mapData)) {
            return mapData;
        }

        ResourceLocation rawId = EntryResolver.getRawId(entryId);
        if (rawId != null && !rawId.equals(entryId)) {
            EntryUnlockData rawMapData = entryUnlockDataMap.get(rawId);
            if (rawMapData != null && !EntryUnlockData.DEFAULT.equals(rawMapData)) {
                return rawMapData;
            }
        }

        if (isKillToUnlock(entryId)) {
            return new EntryUnlockData(false, Collections.emptyList(), List.of(EntryUnlockData.UnlockTrigger.KILL), Collections.emptyList());
        }

        if (isEatToUnlock(entryId)) {
            return new EntryUnlockData(false, Collections.emptyList(), List.of(EntryUnlockData.UnlockTrigger.EAT), Collections.emptyList());
        }

        if ("item".equals(entryId.getNamespace()) && BuiltInRegistries.ITEM.containsKey(EntryResolver.getRawId(entryId))) {
            return new EntryUnlockData(false, Collections.emptyList(), List.of(EntryUnlockData.UnlockTrigger.OBTAIN), Collections.emptyList());
        }

        if ("block".equals(entryId.getNamespace()) && BuiltInRegistries.BLOCK.containsKey(EntryResolver.getRawId(entryId))) {
            return new EntryUnlockData(false, Collections.emptyList(), List.of(EntryUnlockData.UnlockTrigger.SCAN), Collections.emptyList());
        }

        return EntryUnlockData.DEFAULT;
    }

    public Map<ResourceLocation, Category> getCategories() {
        return categories;
    }

    public Map<ResourceLocation, ResourceLocation> getRedirects() {
        return redirects;
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

    public List<String> getLootRemovals() {
        return lootRemovals;
    }

    public boolean hasEntry(ResourceLocation entryId) {
        return EntryResolver.hasEntry(resolvedCategoryEntries, entryId);
    }

    public Map<ResourceLocation, List<Object>> getResolvedEntries() {
        return this.resolvedCategoryEntries;
    }

    public boolean isKillToUnlock(ResourceLocation entryId) {
        TagKey<EntityType<?>> killToUnlockTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(Constants.MOD_ID, "kill_to_unlock"));

        return BuiltInRegistries.ENTITY_TYPE.getOptional(EntryResolver.getRawId(entryId))
                .flatMap(BuiltInRegistries.ENTITY_TYPE::getResourceKey)
                .flatMap(BuiltInRegistries.ENTITY_TYPE::getHolder)
                .map(h -> h.is(killToUnlockTag))
                .orElse(false);
    }

    public boolean isEatToUnlock(ResourceLocation entryId) {
        if ("entity".equals(entryId.getNamespace())) return false;

        TagKey<Item> eatToUnlockTag = TagKey.create(Registries.ITEM, new ResourceLocation(Constants.MOD_ID, "eat_to_unlock"));

        return BuiltInRegistries.ITEM.getOptional(EntryResolver.getRawId(entryId))
                .map(item -> item.isEdible() || BuiltInRegistries.ITEM.getHolderOrThrow(BuiltInRegistries.ITEM.getResourceKey(item).get()).is(eatToUnlockTag))
                .orElse(false);
    }

    public Set<ResourceLocation> getEntriesTriggeredBy(ResourceLocation triggerId) {
        return triggerOnMap.getOrDefault(triggerId, Collections.emptySet());
    }

    public ResourceLocation getCategoryForEntryId(ResourceLocation entryId) {
        return EntryResolver.getCategoryForEntryId(resolvedCategoryEntries, entryId);
    }

    public Set<ResourceLocation> getAllEntryIds() {
        return EntryResolver.getAllEntryIds(resolvedCategoryEntries);
    }

    public Set<ResourceLocation> getEntryIdsForCategory(ResourceLocation categoryId) {
        return EntryResolver.getEntryIdsForCategory(resolvedCategoryEntries, categoryId);
    }

    public List<CompositeDefinition> getComposites() {
        return composites;
    }

    public Map<ResourceLocation, List<DatapackVariant>> getVariants() {
        return variants;
    }

    public List<Object> getEntriesForTarget(Object target) {
        return EntryResolver.getEntriesForTarget(resolvedCategoryEntries, target);
    }

    public boolean isTargetInEntry(ResourceLocation targetId, ResourceLocation entryId) {
        return EntryResolver.isTargetInEntry(resolvedCategoryEntries, targetId, entryId);
    }

    private void calculatePrefixedLists() {
        this.prefixedBiomeAdditions = prefixList(biomeAdditions);
        this.prefixedLootAdditions = prefixList(lootAdditions);
    }

    private List<String> prefixList(List<String> original) {
        return original.stream().map(s -> {
            String[] parts = s.split("\\|", 2);
            if (parts.length == 2 && !parts[0].contains(":")) {
                Optional<Object> entry = EntryResolutionHelper.resolveSingleEntry(new ResourceLocation(parts[0]), null, null);
                if (entry.isPresent()) return AutoPopulateRegistry.getEntryId(entry.get(), true) + "|" + parts[1];
            }
            return s;
        }).toList();
    }

    public void syncToPlayer(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(new SyncConfigPacket(ServerConfig.get()), player);

        List<Category> flattenedCategories = new ArrayList<>();
        List<GuideEntry> flattenedEntries = new ArrayList<>();
        int maxEntriesPerChunk = 100;

        for (Category rawCat : categories.values()) {
            List<Object> resolved = resolvedCategoryEntries.get(rawCat.getId());

            if (resolved == null || resolved.isEmpty()) {
                Category flatCat = new Category(rawCat.getId());
                flatCat.setSortIndex(rawCat.getSortIndex());
                flatCat.setIcon(rawCat.getIcon());
                flatCat.setGroupByQueries(rawCat.getGroupByQueries());
                flattenedCategories.add(flatCat);
                continue;
            }

            int index = 0;
            while (index < resolved.size()) {
                Category chunkCat = new Category(rawCat.getId());
                chunkCat.setSortIndex(rawCat.getSortIndex());
                chunkCat.setIcon(rawCat.getIcon());

                if (index == 0) {
                    chunkCat.setGroupByQueries(rawCat.getGroupByQueries());
                }

                int endIndex = Math.min(index + maxEntriesPerChunk, resolved.size());
                for (int j = index; j < endIndex; j++) {
                    Object obj = resolved.get(j);
                    ResourceLocation id = AutoPopulateRegistry.getEntryId(obj, true);

                    if (obj instanceof GuideEntry ge) {
                        chunkCat.addEntryId(ge.id());
                        if (!flattenedEntries.contains(ge)) flattenedEntries.add(ge);
                        continue;
                    }

                    // Fallback to synthesize missing explicit GuideEntry wrappers for standard objects
                    GuideEntry existing = allEntries.get(id);
                    if (existing != null) {
                        chunkCat.addEntryId(id);
                        if (!flattenedEntries.contains(existing)) flattenedEntries.add(existing);
                    } else {
                        EntryUnlockData unlockData = getUnlockData(id);

                        GuideEntry synthesized = new GuideEntry(id, id, null, EntryKind.NORMAL, false, false, null, null, null, null, unlockData);
                        chunkCat.addEntryId(id);
                        if (!flattenedEntries.contains(synthesized)) flattenedEntries.add(synthesized);
                    }
                }
                flattenedCategories.add(chunkCat);
                index += maxEntriesPerChunk;
            }
        }

        Services.NETWORK.sendToPlayer(new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), variants, true, false), player);

        sendChunked(player, prefixedBiomeAdditions, (chunk) -> new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), chunk, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), false, false));
        sendChunked(player, biomeRemovals, (chunk) -> new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), chunk, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), false, false));
        sendChunked(player, prefixedLootAdditions, (chunk) -> new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), chunk, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), false, false));
        sendChunked(player, lootRemovals, (chunk) -> new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), chunk, Collections.emptyMap(), Collections.emptyMap(), false, false));

        if (!redirects.isEmpty()) {
            int maxModifiersChunkSize = 500;
            Map<ResourceLocation, ResourceLocation> redChunk = new HashMap<>();
            int count = 0;
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : redirects.entrySet()) {
                redChunk.put(entry.getKey(), entry.getValue());
                count++;
                if (count >= maxModifiersChunkSize) {
                    Services.NETWORK.sendToPlayer(new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), redChunk, Collections.emptyMap(), false, false), player);
                    redChunk = new HashMap<>();
                    count = 0;
                }
            }
            if (!redChunk.isEmpty()) {
                Services.NETWORK.sendToPlayer(new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), redChunk, Collections.emptyMap(), false, false), player);
            }
        }

        if (flattenedCategories.isEmpty()) {
            Services.NETWORK.sendToPlayer(new SyncCategoriesPacket(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), false, true), player);
        } else {
            for (int i = 0; i < flattenedCategories.size(); i++) {
                List<Category> catChunk = Collections.singletonList(flattenedCategories.get(i));
                List<GuideEntry> entryChunk = new ArrayList<>();
                for (ResourceLocation entryId : flattenedCategories.get(i).getEntryIds()) {
                    flattenedEntries.stream().filter(e -> e.id().equals(entryId)).findFirst().ifPresent(entryChunk::add);
                }

                boolean isLast = (i == flattenedCategories.size() - 1);
                Services.NETWORK.sendToPlayer(new SyncCategoriesPacket(catChunk, entryChunk, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), false, isLast), player);
            }
        }

        syncLootToPlayer(player);
    }

    public void syncLootToPlayer(ServerPlayer player) {
        if (serverLootCache.isEmpty()) return;

        int maxLootChunkSize = 100;
        Map<ResourceLocation, List<ItemStack>> chunk = new HashMap<>();
        int count = 0;

        for (Map.Entry<ResourceLocation, List<ItemStack>> entry : serverLootCache.entrySet()) {
            chunk.put(entry.getKey(), entry.getValue());
            count++;

            if (count >= maxLootChunkSize) {
                Services.NETWORK.sendToPlayer(new SyncLootPacket(chunk, false), player);
                chunk = new HashMap<>();
                count = 0;
            }
        }

        if (!chunk.isEmpty()) {
            Services.NETWORK.sendToPlayer(new SyncLootPacket(chunk, false), player);
        }
    }

    public void syncLootToPlayer(ServerPlayer player, ResourceLocation entryId) {
        if (serverLootCache.containsKey(entryId)) {
            Services.NETWORK.sendToPlayer(new SyncLootPacket(Map.of(entryId, serverLootCache.get(entryId)), false), player);
        }
    }

    private <T> void sendChunked(ServerPlayer player, List<T> list, java.util.function.Function<List<T>, Object> packetFactory) {
        int maxChunkSize = 500;
        for (int i = 0; i < list.size(); i += maxChunkSize) {
            List<T> chunk = list.subList(i, Math.min(i + maxChunkSize, list.size()));
            Services.NETWORK.sendToPlayer(packetFactory.apply(chunk), player);
        }
    }

    private void resolveAllCategories() {
        resolvedCategoryEntries.clear();
        Set<String> allCompositeComponents = new HashSet<>();

        for (Category cat : categories.values()) {
            List<Object> entries = EntryResolutionHelper.resolveCategoryEntries(cat, allEntries, composites, redirects);
            for (Object entry : entries) {
                if (entry instanceof GuideEntry ge && ge.isComposite()) {
                    if (ge.childEntries() != null) {
                        for (ResourceLocation comp : ge.childEntries()) {
                            allCompositeComponents.add(comp.toString());
                        }
                    }
                }
            }
            resolvedCategoryEntries.put(cat.getId(), entries);
        }

        for (List<Object> entries : resolvedCategoryEntries.values()) {
            entries.removeIf(entry -> {
                if (entry instanceof GuideEntry ge && ge.isComposite()) return false;
                return allCompositeComponents.contains(AutoPopulateRegistry.getEntryKey(entry));
            });
        }

        registerResolvedEntryUnlocks();
    }

    private void registerResolvedEntryUnlocks() {
        for (List<Object> entries : resolvedCategoryEntries.values()) {
            for (Object obj : entries) {
                if (!(obj instanceof GuideEntry ge)) continue;
                if (EntryUnlockData.DEFAULT.equals(ge.unlockData())) continue;
                entryUnlockDataMap.putIfAbsent(ge.id(), ge.unlockData());
            }
        }

        for (Map.Entry<ResourceLocation, EntryUnlockData> mapEntry : entryUnlockDataMap.entrySet()) {
            ResourceLocation entryId = mapEntry.getKey();
            for (ResourceLocation triggerId : mapEntry.getValue().triggerOn()) {
                triggerOnMap.computeIfAbsent(triggerId, k -> new HashSet<>()).add(entryId);
            }
        }
    }

    public void onServerStarted(MinecraftServer server) {
        resolveAllCategories();
        StaticLootParser.clearCache();
        this.serverLootCache = LootTableHelper.generateLootMap(server.overworld());
        expandBiomeTags(server);
        expandItemTags(server);
        generateAutoBiomeAdditions(server);
        calculatePrefixedLists();
    }

    public void reload(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Services.NETWORK.sendToPlayer(new ExportContentPacket("reload_cache"), player);
        }

        resolveAllCategories();
        StaticLootParser.clearCache();
        this.serverLootCache = LootTableHelper.generateLootMap(server.overworld());
        expandBiomeTags(server);
        expandItemTags(server);
        generateAutoBiomeAdditions(server);
        calculatePrefixedLists();

        syncToAll(server);
    }

    public Category getCategoryForEntry(Object entry) {
        for (Map.Entry<ResourceLocation, List<Object>> cat : resolvedCategoryEntries.entrySet()) {
            if (cat.getValue().contains(entry)) return categories.get(cat.getKey());
        }
        return null;
    }

    @Override
    protected @NotNull ReloadData prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        ReloadData data = new ReloadData();

        loadCategories(resourceManager, data);
        loadComposites(resourceManager, data);
        loadModifiers(resourceManager, "fieldguide/biome_modifiers", data.biomeAdditions, data.biomeRemovals);
        loadModifiers(resourceManager, "fieldguide/loot_modifiers", data.lootAdditions, data.lootRemovals);
        loadRedirects(resourceManager, data);
        loadVariants(resourceManager, data);

        if (Services.PLATFORM.isModLoaded("cobblemon")) {
            FieldGuideCobblemonCompat.populateCache(resourceManager);
        }

        return data;
    }

    private void loadCategories(ResourceManager resourceManager, ReloadData data) {
        Map<ResourceLocation, List<Resource>> categoryResources = resourceManager.listResourceStacks(
                "fieldguide/categories",
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, List<Resource>> entry : categoryResources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            String idPath = path.substring("fieldguide/categories/".length(), path.length() - ".json".length());
            ResourceLocation defaultCategoryId = new ResourceLocation(fileId.getNamespace(), idPath);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    ResourceLocation categoryId = defaultCategoryId;
                    if (json.has("target_category")) {
                        categoryId = new ResourceLocation(GsonHelper.getAsString(json, "target_category"));
                    }

                    Category category = data.categories.computeIfAbsent(categoryId, Category::new);

                    if (GsonHelper.getAsBoolean(json, "hidden", false)) {
                        data.categories.remove(categoryId);
                        continue;
                    }

                    if (GsonHelper.getAsBoolean(json, "replace", false)) {
                        category.getEntryIds().clear();
                    }

                    if (json.has("sort_index")) {
                        category.setSortIndex(GsonHelper.getAsInt(json, "sort_index"));
                    }

                    if (json.has("icon")) {
                        category.setIcon(new ResourceLocation(GsonHelper.getAsString(json, "icon")));
                    }

                    if (json.has("group_by")) {
                        JsonArray groupBy = GsonHelper.getAsJsonArray(json, "group_by");
                        List<String> queries = new ArrayList<>(groupBy.size());
                        for (JsonElement query : groupBy) {
                            queries.add(query.getAsString());
                        }

                        category.setGroupByQueries(queries);
                    }

                    if (json.has("contents")) {
                        JsonArray contents = GsonHelper.getAsJsonArray(json, "contents");
                        for (JsonElement el : contents) {
                            JsonObject obj = el.getAsJsonObject();
                            String typeStr = GsonHelper.getAsString(obj, "type");
                            EntryUnlockData unlockData = parseUnlockData(obj);

                            switch (typeStr) {
                                case "entry" -> {
                                    ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(obj, "id"));
                                    GuideEntry ge = new GuideEntry(id, id, null, EntryKind.NORMAL, false, false, null, null, null, null, unlockData);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    data.entryUnlockData.put(id, unlockData);
                                }
                                case "virtual_entry" -> {
                                    ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(obj, "id"));
                                    String virtualType = GsonHelper.getAsString(obj, "virtual_type");
                                    ResourceLocation icon = obj.has("icon") ? new ResourceLocation(GsonHelper.getAsString(obj, "icon")) : null;
                                    GuideEntry ge = new GuideEntry(id, null, icon, EntryKind.NORMAL, true, false, null, null, null, new VirtualData(virtualType), unlockData);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    data.entryUnlockData.put(id, unlockData);
                                }
                                case "auto_populate" -> {
                                    String strategy = GsonHelper.getAsString(obj, "strategy");
                                    String safeStrategyName = strategy.replace(":", "_");
                                    ResourceLocation id = new ResourceLocation(categoryId.getNamespace(), categoryId.getPath() + "_auto_" + safeStrategyName);
                                    GuideEntry ge = new GuideEntry(id, null, null, EntryKind.NORMAL, false, true, strategy, null, null, null, unlockData);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load category: {}", fileId, e);
                }
            }
        }
    }

    private EntryUnlockData parseUnlockData(JsonObject obj) {
        if (!obj.has("unlock")) return EntryUnlockData.DEFAULT;
        JsonObject unlock = obj.getAsJsonObject("unlock");

        boolean unlockedByDefault = GsonHelper.getAsBoolean(unlock, "unlocked_by_default", false);
        List<ResourceLocation> prerequisites = new ArrayList<>();
        if (unlock.has("prerequisites")) {
            for (JsonElement e : unlock.getAsJsonArray("prerequisites")) {
                prerequisites.add(new ResourceLocation(e.getAsString()));
            }
        }

        List<EntryUnlockData.UnlockTrigger> triggers = new ArrayList<>();
        if (unlock.has("triggers")) {
            for (JsonElement e : unlock.getAsJsonArray("triggers")) {
                String triggerStr = e.getAsString().toUpperCase();
                try {
                    triggers.add(EntryUnlockData.UnlockTrigger.valueOf(triggerStr));
                } catch (IllegalArgumentException ex) {
                    Constants.LOG.error("Unknown unlock trigger: {}", triggerStr);
                }
            }
        }

        List<ResourceLocation> triggerOn = new ArrayList<>();
        if (unlock.has("trigger_on")) {
            JsonElement e = unlock.get("trigger_on");
            if (e.isJsonArray()) {
                for (JsonElement el : e.getAsJsonArray()) {
                    triggerOn.add(new ResourceLocation(el.getAsString()));
                }
            } else {
                triggerOn.add(new ResourceLocation(e.getAsString()));
            }
        }

        return new EntryUnlockData(unlockedByDefault, prerequisites, triggers, triggerOn);
    }

    private void loadComposites(ResourceManager resourceManager, ReloadData data) {
        Map<ResourceLocation, List<Resource>> compositeResources = resourceManager.listResourceStacks(
                "fieldguide/composites",
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, List<Resource>> entry : compositeResources.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    if (GsonHelper.getAsBoolean(json, "replace", false)) {
                        data.composites.clear();
                    }

                    if (json.has("values")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "values")) {
                            JsonObject obj = el.getAsJsonObject();
                            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(obj, "id"));

                            if (GsonHelper.getAsBoolean(obj, "replace", false)) {
                                data.composites.removeIf(c -> c.id().equals(id));
                            }

                            ResourceLocation displayId = obj.has("display") ? new ResourceLocation(GsonHelper.getAsString(obj, "display")) : id;

                            List<ResourceLocation> components = new ArrayList<>();
                            if (obj.has("components")) {
                                for (JsonElement comp : GsonHelper.getAsJsonArray(obj, "components")) {
                                    components.add(new ResourceLocation(comp.getAsString()));
                                }
                            }

                            ResourceLocation structureNbt = obj.has("structure_nbt") ? new ResourceLocation(GsonHelper.getAsString(obj, "structure_nbt")) : null;

                            List<String> stackedBlocks = null;
                            if (obj.has("render")) {
                                stackedBlocks = new ArrayList<>();
                                for (JsonElement el2 : GsonHelper.getAsJsonArray(obj, "render")) {
                                    stackedBlocks.add(el2.getAsString());
                                }
                            }

                            data.composites.add(new CompositeDefinition(id, displayId, components, structureNbt, stackedBlocks));
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load composite: {}", entry.getKey(), e);
                }
            }
        }
    }

    private void loadRedirects(ResourceManager resourceManager, ReloadData data) {
        Map<ResourceLocation, List<Resource>> redirectResources = resourceManager.listResourceStacks(
                "fieldguide/redirects",
                id -> id.getPath().endsWith(".json")
        );
        for (Map.Entry<ResourceLocation, List<Resource>> entry : redirectResources.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);
                    if (json.has("entries")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "entries")) {
                            JsonObject obj = el.getAsJsonObject();
                            ResourceLocation source = new ResourceLocation(GsonHelper.getAsString(obj, "source"));
                            ResourceLocation target = new ResourceLocation(GsonHelper.getAsString(obj, "target"));
                            data.redirects.put(source, target);
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load redirect: {}", entry.getKey(), e);
                }
            }
        }
    }

    private void loadVariants(ResourceManager resourceManager, ReloadData data) {
        Map<ResourceLocation, List<Resource>> variantResources = resourceManager.listResourceStacks(
                "fieldguide/variants",
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, List<Resource>> entry : variantResources.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    if (json.has("entries")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "entries")) {
                            JsonObject obj = el.getAsJsonObject();
                            ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(obj, "id"));

                            List<DatapackVariant> variantList = data.variants.computeIfAbsent(entityId, k -> new ArrayList<>());

                            if (GsonHelper.getAsBoolean(obj, "replace", false)) {
                                variantList.clear();
                            }

                            if (obj.has("variants")) {
                                for (JsonElement vEl : GsonHelper.getAsJsonArray(obj, "variants")) {
                                    JsonObject vObj = vEl.getAsJsonObject();
                                    String id = GsonHelper.getAsString(vObj, "id");
                                    CompoundTag nbt = TagParser.parseTag(GsonHelper.getAsString(vObj, "nbt"));
                                    variantList.add(new DatapackVariant(id, nbt));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load variant: {}", entry.getKey(), e);
                }
            }
        }
    }

    private void generateAutoBiomeAdditions(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        Set<String> additionsSet = new LinkedHashSet<>(this.biomeAdditions);

        for (var biomeEntry : biomeRegistry.entrySet()) {
            try {
                ResourceLocation biomeId = biomeEntry.getKey().location();
                Biome biome = biomeEntry.getValue();

                for (MobCategory cat : MobCategory.values()) {
                    for (var spawn : biome.getMobSettings().getMobs(cat).unwrap()) {
                        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type);
                        additionsSet.add(entityId + "|" + biomeId);
                    }
                }
            } catch (IllegalStateException e) {
                Constants.LOG.warn("Skipping unbound biome in registry: {}", biomeEntry.getKey().location());
            }
        }

        this.biomeAdditions = new ArrayList<>(additionsSet);
    }

    private void loadModifiers(ResourceManager resourceManager, String path, List<String> additions, List<String> removals) {
        Map<ResourceLocation, List<Resource>> resources = resourceManager.listResourceStacks(path, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, List<Resource>> entry : resources.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    if (GsonHelper.getAsBoolean(json, "replace", false)) {
                        additions.clear();
                        removals.clear();
                    }

                    if (json.has("additions")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "additions")) {
                            JsonObject obj = el.getAsJsonObject();
                            List<String> entryList = getAsList(obj, "entry", "entries");
                            List<String> valueList = getAsList(obj, "value", "values");

                            for (String e : entryList) {
                                for (String v : valueList) {
                                    additions.add(e + "|" + v);
                                }
                            }
                        }
                    }
                    if (json.has("removals")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "removals")) {
                            JsonObject obj = el.getAsJsonObject();
                            List<String> entryList = getAsList(obj, "entry", "entries");
                            List<String> valueList = getAsList(obj, "value", "values");

                            for (String e : entryList) {
                                for (String v : valueList) {
                                    removals.add(e + "|" + v);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load modifiers from {}", entry.getKey(), e);
                }
            }
        }
    }

    private List<String> getAsList(JsonObject obj, String singular, String plural) {
        List<String> list = new ArrayList<>();
        if (obj.has(singular)) {
            JsonElement el = obj.get(singular);
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) list.add(e.getAsString());
            } else {
                list.add(el.getAsString());
            }
        }
        if (obj.has(plural)) {
            JsonElement el = obj.get(plural);
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) list.add(e.getAsString());
            } else {
                list.add(el.getAsString());
            }
        }
        return list;
    }

    private void expandBiomeTags(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
        this.biomeAdditions = expandTagsForList(this.biomeAdditions, biomeRegistry, Registries.BIOME);
        this.biomeRemovals = expandTagsForList(this.biomeRemovals, biomeRegistry, Registries.BIOME);
    }

    private void expandItemTags(MinecraftServer server) {
        this.lootAdditions = expandTagsForList(this.lootAdditions, BuiltInRegistries.ITEM, Registries.ITEM);
        this.lootRemovals = expandTagsForList(this.lootRemovals, BuiltInRegistries.ITEM, Registries.ITEM);
    }

    private <T> List<String> expandTagsForList(List<String> list, Registry<T> registry, ResourceKey<? extends Registry<T>> registryKey) {
        Set<String> expanded = new LinkedHashSet<>();

        for (String item : list) {
            String[] parts = item.split("\\|", 2);
            if (parts.length == 2 && parts[1].startsWith("#")) {
                String tagPath = parts[1].substring(1);
                try {
                    TagKey<T> tagKey = TagKey.create(registryKey, new ResourceLocation(tagPath));
                    registry.getTagOrEmpty(tagKey).forEach(holder -> {
                        holder.unwrapKey().ifPresent(key -> {
                            expanded.add(parts[0] + "|" + key.location());
                        });
                    });
                } catch (Exception e) {
                    Constants.LOG.error("Failed to expand tag: {}", parts[1], e);
                    expanded.add(item);
                }
            } else {
                expanded.add(item);
            }
        }

        return new ArrayList<>(expanded);
    }

    public void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    @Override
    protected void apply(@NotNull ReloadData data, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        ModConfig.load();
        this.categories = data.categories;
        this.allEntries = data.allEntries;
        this.composites = data.composites;
        this.biomeAdditions = data.biomeAdditions;
        this.biomeRemovals = data.biomeRemovals;
        this.lootAdditions = data.lootAdditions;
        this.lootRemovals = data.lootRemovals;
        this.redirects = data.redirects;
        this.variants = data.variants;
        this.entryUnlockDataMap.clear();
        this.entryUnlockDataMap.putAll(data.entryUnlockData);

        this.triggerOnMap.clear();
        for (Map.Entry<ResourceLocation, EntryUnlockData> entry : this.entryUnlockDataMap.entrySet()) {
            ResourceLocation entryId = entry.getKey();
            for (ResourceLocation triggerId : entry.getValue().triggerOn()) {
                this.triggerOnMap.computeIfAbsent(triggerId, k -> new HashSet<>()).add(entryId);
            }
        }
    }

    public static class ReloadData {
        public Map<ResourceLocation, Category> categories = new LinkedHashMap<>();
        public Map<ResourceLocation, GuideEntry> allEntries = new HashMap<>();
        public List<CompositeDefinition> composites = new ArrayList<>();
        public List<String> biomeAdditions = new ArrayList<>();
        public List<String> biomeRemovals = new ArrayList<>();
        public List<String> lootAdditions = new ArrayList<>();
        public List<String> lootRemovals = new ArrayList<>();
        public Map<ResourceLocation, ResourceLocation> redirects = new HashMap<>();
        public Map<ResourceLocation, List<DatapackVariant>> variants = new HashMap<>();
        public Map<ResourceLocation, EntryUnlockData> entryUnlockData = new HashMap<>();
    }
}
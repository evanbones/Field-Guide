package com.evandev.fieldguide.client.search;

import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.seasons.SeasonsAPI;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.manager.ClientLootManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class SearchManager {

    public static List<Object> groupByQueries(List<Object> entries, List<String> queries) {
        List<Object> remainingEntries = new ArrayList<>(entries);
        List<Object> groupedResults = new ArrayList<>(entries.size());

        for (String query : queries) {
            List<Object> results = getMatches(query, remainingEntries);

            groupedResults.addAll(results);

            Set<Object> resultSet = new HashSet<>(results);
            remainingEntries.removeIf(resultSet::contains);
        }

        groupedResults.addAll(remainingEntries);
        return groupedResults;
    }

    public static List<Object> searchEntries(String query) {
        return searchEntries(query, ClientFieldGuideManager.getValidEntries());
    }

    public static List<Object> searchEntries(String query, List<Object> entries) {
        List<Object> rawMatches = getMatches(query, entries);
        List<Object> visibleResults = new ArrayList<>();

        for (Object match : rawMatches) {
            if (!ClientFieldGuideManager.hideFromSearch(match)) {
                visibleResults.add(match);
            }
        }

        return visibleResults;
    }

    private static List<Object> getMatches(String query, List<Object> entries) {
        String processedQuery = query.toLowerCase(Locale.ROOT).trim();
        List<Object> results = new ArrayList<>();
        if (processedQuery.isEmpty()) return results;

        boolean exactMatch = false;
        if (processedQuery.startsWith("=")) {
            exactMatch = true;
            processedQuery = processedQuery.substring(1);
        }

        if (processedQuery.startsWith("#")) return matchByTag(processedQuery.substring(1), entries, exactMatch);
        if (processedQuery.startsWith("^")) return matchByDrop(processedQuery.substring(1), entries, exactMatch);
        if (processedQuery.startsWith("!")) return matchByBiome(processedQuery.substring(1), entries, exactMatch);
        if (processedQuery.startsWith("@")) return matchByModId(processedQuery.substring(1), entries, exactMatch);
        if (processedQuery.startsWith("$")) return matchBySeason(processedQuery.substring(1), entries, exactMatch);

        return matchByNameOrId(processedQuery, entries, exactMatch);
    }

    private static List<Object> matchBySeason(String seasonQuery, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        if (seasonQuery.isEmpty()) return results;

        for (Object entry : entries) {
            List<Season> seasons = SeasonsAPI.getGrowingSeasons(entry);
            boolean match = seasons.stream().anyMatch(season -> {
                String id = season.getId().toLowerCase(Locale.ROOT);
                String name = season.getDisplayName().getString().toLowerCase(Locale.ROOT);
                return exactMatch ? (id.equals(seasonQuery) || name.equals(seasonQuery)) : (id.contains(seasonQuery) || name.contains(seasonQuery));
            });
            if (match) results.add(entry);
        }
        return results;
    }

    private static List<Object> matchByTag(String tagQuery, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        if (tagQuery.isEmpty()) return results;

        for (Object entry : entries) {
            Object coreEntry = EntryResolver.resolveCoreEntry(entry);

            if (coreEntry instanceof EntityType<?> type) {
                var key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(type);
                key.flatMap(BuiltInRegistries.ENTITY_TYPE::getHolder).ifPresent(holder -> {
                    if (holder.tags().anyMatch(tag -> matchLocation(tag.location(), tagQuery, exactMatch)))
                        results.add(entry);
                });
            } else if (coreEntry instanceof Block block) {
                var key = BuiltInRegistries.BLOCK.getResourceKey(block);
                key.flatMap(BuiltInRegistries.BLOCK::getHolder).ifPresent(holder -> {
                    if (holder.tags().anyMatch(tag -> matchLocation(tag.location(), tagQuery, exactMatch)))
                        results.add(entry);
                });
            } else if (coreEntry instanceof Item item) {
                var key = BuiltInRegistries.ITEM.getResourceKey(item);
                key.flatMap(BuiltInRegistries.ITEM::getHolder).ifPresent(holder -> {
                    if (holder.tags().anyMatch(tag -> matchLocation(tag.location(), tagQuery, exactMatch)))
                        results.add(entry);
                });
            }
        }
        return results;
    }

    private static List<Object> matchByDrop(String dropQuery, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        if (dropQuery.isEmpty()) return results;

        List<Object> drops = ClientLootManager.getInstance().getEntriesDropping(dropQuery, exactMatch);
        for (Object entry : drops) {
            if (entries.contains(entry) && !results.contains(entry)) {
                results.add(entry);
            }
        }
        return results;
    }

    private static List<Object> matchByBiome(String biomeQuery, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        if (biomeQuery.isEmpty()) return results;

        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var biomeRegistry = connection.registryAccess().registryOrThrow(Registries.BIOME);

            for (var biomeEntry : biomeRegistry.entrySet()) {
                if (matchLocation(biomeEntry.getKey().location(), biomeQuery, exactMatch)) {
                    try {
                        Biome biome = biomeEntry.getValue();

                        for (MobCategory cat : MobCategory.values()) {
                            for (var spawn : biome.getMobSettings().getMobs(cat).unwrap()) {

                                Object entry = ClientFieldGuideManager.getInstance().getEntryForTarget(spawn.type);
                                ResourceLocation categoryId = null;

                                if (entry != null) {
                                    var category = ClientFieldGuideManager.getInstance().getCategoryForEntry(entry);
                                    if (category != null) {
                                        categoryId = category.getId();
                                    }
                                }

                                if (EntryResolver.isValidEntity(spawn.type, categoryId)) {
                                    if (entry != null && !results.contains(entry) && entries.contains(entry)) {
                                        results.add(entry);
                                    }
                                }
                            }
                        }

                        if (Services.PLATFORM.isModLoaded("cobblemon")) {
                            for (Object entry : entries) {
                                if (ClientFieldGuideCobblemonCompat.isCobblemonBiomeMatch(entry, biomeEntry.getKey().location(), biomeRegistry.getHolderOrThrow(biomeEntry.getKey()))) {
                                    if (!results.contains(entry)) {
                                        results.add(entry);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        for (Object entry : entries) {
            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
            if (entryId != null) {
                for (String addition : ClientFieldGuideManager.getInstance().getBiomeAdditions()) {
                    String[] parts = addition.split("\\|");
                    if (parts.length == 2 && parts[0].equals(entryId.toString())) {
                        ResourceLocation biomeId = new ResourceLocation(parts[1]);
                        if (matchLocation(biomeId, biomeQuery, exactMatch)) {
                            if (!results.contains(entry)) results.add(entry);
                        }
                    }
                }
            }
        }

        for (Object entry : new ArrayList<>(results)) {
            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
            if (entryId != null) {
                for (String removal : ClientFieldGuideManager.getInstance().getBiomeRemovals()) {
                    String[] parts = removal.split("\\|");
                    if (parts.length == 2 && parts[0].equals(entryId.toString())) {
                        ResourceLocation biomeId = new ResourceLocation(parts[1]);
                        if (matchLocation(biomeId, biomeQuery, exactMatch)) {
                            results.remove(entry);
                        }
                    }
                }
            }
        }

        return results;
    }

    private static List<Object> matchByModId(String modQuery, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        if (modQuery.isEmpty()) return results;

        for (Object entry : entries) {
            ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
            if (id != null) {
                ResourceLocation rawId = EntryResolver.getRawId(id);

                String namespace = rawId.getNamespace().toLowerCase(Locale.ROOT);
                if (exactMatch ? namespace.equals(modQuery) : namespace.contains(modQuery)) results.add(entry);
            }
        }
        return results;
    }

    private static List<Object> matchByNameOrId(String query, List<Object> entries, boolean exactMatch) {
        List<Object> results = new ArrayList<>();
        for (Object entry : entries) {
            ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
            if (id == null) continue;

            String name = ClientFieldGuideManager.getDefaultName(entry).toLowerCase(Locale.ROOT);
            ResourceLocation rawId = EntryResolver.getRawId(id);
            String rawIdStr = rawId != null ? rawId.toString().toLowerCase(Locale.ROOT) : "";
            String rawPath = rawId != null ? rawId.getPath().toLowerCase(Locale.ROOT) : "";
            String fullIdStr = id.toString().toLowerCase(Locale.ROOT);
            String path = id.getPath().toLowerCase(Locale.ROOT);

            boolean match = exactMatch ?
                    (name.equals(query) || path.equals(query) || rawPath.equals(query) || fullIdStr.equals(query) || rawIdStr.equals(query)) :
                    (name.contains(query) || path.contains(query) || rawPath.contains(query) || fullIdStr.contains(query) || rawIdStr.contains(query));

            if (match) results.add(entry);
        }
        return results;
    }

    private static boolean matchLocation(ResourceLocation loc, String query, boolean exactMatch) {
        String full = loc.toString().toLowerCase(Locale.ROOT);
        String path = loc.getPath().toLowerCase(Locale.ROOT);
        return exactMatch ? (full.equals(query) || path.equals(query)) : (full.contains(query) || path.contains(query));
    }
}
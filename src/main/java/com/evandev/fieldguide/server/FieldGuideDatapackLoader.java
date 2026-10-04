package com.evandev.fieldguide.server;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.*;
import com.evandev.fieldguide.api.variant.DatapackVariant;
import com.evandev.fieldguide.api.variant.DatapackVariantDefinition;
import com.evandev.fieldguide.api.variant.VariantConditionEvaluator;
import com.evandev.fieldguide.entry.EntryResolver;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class FieldGuideDatapackLoader {

    private FieldGuideDatapackLoader() {
    }

    public static ServerFieldGuideManager.ReloadData load(ResourceManager resourceManager) {
        ServerFieldGuideManager.ReloadData data = new ServerFieldGuideManager.ReloadData();

        loadCategories(resourceManager, data);
        loadComposites(resourceManager, data);
        loadModifiers(resourceManager, "fieldguide/biome_modifiers", data.biomeAdditions, data.biomeRemovals);
        loadModifiers(resourceManager, "fieldguide/loot_modifiers", data.lootAdditions, data.lootRemovals);
        loadRedirects(resourceManager, data);
        loadVariants(resourceManager, data);

        return data;
    }

    private static void loadCategories(ResourceManager resourceManager, ServerFieldGuideManager.ReloadData data) {
        Map<ResourceLocation, List<Resource>> categoryResources = resourceManager.listResourceStacks(
                "fieldguide/categories",
                id -> id.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, List<Resource>> entry : categoryResources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            String idPath = path.substring("fieldguide/categories/".length(), path.length() - ".json".length());
            ResourceLocation defaultCategoryId = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), idPath);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    ResourceLocation categoryId = defaultCategoryId;
                    if (json.has("target_category")) {
                        categoryId = ResourceLocation.parse(GsonHelper.getAsString(json, "target_category"));
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
                        category.setIcon(ResourceLocation.parse(GsonHelper.getAsString(json, "icon")));
                    }

                    if (json.has("group_by")) {
                        JsonArray groupBy = GsonHelper.getAsJsonArray(json, "group_by");
                        List<String> queries = new ArrayList<>(groupBy.size());
                        for (JsonElement query : groupBy) {
                            queries.add(query.getAsString());
                        }

                        category.setGroupByQueries(queries);
                    }

                    EntryUnlockData categoryUnlockData = parseUnlockData(json);

                    if (json.has("contents")) {
                        JsonArray contents = GsonHelper.getAsJsonArray(json, "contents");
                        for (JsonElement el : contents) {
                            JsonObject obj = el.getAsJsonObject();
                            String typeStr = GsonHelper.getAsString(obj, "type");
                            EntryUnlockData unlockData = obj.has("unlock") ? parseUnlockData(obj) : categoryUnlockData;

                            switch (typeStr) {
                                case "entry" -> {
                                    ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(obj, "id"));
                                    GuideEntry ge = new GuideEntry(id, id, null, EntryKind.NORMAL, false, false, null, null, null, null, null, unlockData, null, null);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    if (unlockData != null && !EntryUnlockData.DEFAULT.equals(unlockData)) {
                                        data.entryUnlockData.put(id, unlockData);
                                    }
                                }
                                case "virtual_entry" -> {
                                    ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(obj, "id"));
                                    String virtualType = GsonHelper.getAsString(obj, "virtual_type");
                                    ResourceLocation icon = obj.has("icon") ? ResourceLocation.parse(GsonHelper.getAsString(obj, "icon")) : null;
                                    GuideEntry ge = new GuideEntry(id, null, icon, EntryKind.NORMAL, true, false, null, null, null, null, new VirtualData(virtualType), unlockData, null, null);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    if (unlockData != null && !EntryUnlockData.DEFAULT.equals(unlockData)) {
                                        data.entryUnlockData.put(id, unlockData);
                                    }
                                }
                                case "auto_populate" -> {
                                    String strategy = GsonHelper.getAsString(obj, "strategy");
                                    String safeStrategyName = strategy.replace(":", "_");
                                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(categoryId.getNamespace(), categoryId.getPath() + "_auto_" + safeStrategyName);
                                    GuideEntry ge = new GuideEntry(id, null, null, EntryKind.NORMAL, false, true, strategy, null, null, null, null, unlockData, null, null);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    if (unlockData != null && !EntryUnlockData.DEFAULT.equals(unlockData)) {
                                        data.entryUnlockData.put(id, unlockData);
                                    }
                                }
                                case "nbt_entry" -> {
                                    ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(obj, "id"));
                                    ResourceLocation entityType = ResourceLocation.parse(GsonHelper.getAsString(obj, "entity_type"));
                                    //? if <26.1 {
                                    CompoundTag nbt = TagParser.parseTag(GsonHelper.getAsString(obj, "nbt"));
                                    //?} else {
                                    /*CompoundTag nbt = TagParser.parseCompoundFully(GsonHelper.getAsString(obj, "nbt"));
                                    *///?}
                                    ResourceLocation displayId = obj.has("display") ? ResourceLocation.parse(GsonHelper.getAsString(obj, "display")) : entityType;
                                    GuideEntry ge = new GuideEntry(id, displayId, null, EntryKind.NORMAL, false, false, null, null, null, null, null, unlockData, entityType, nbt);
                                    data.allEntries.put(id, ge);
                                    category.addEntryId(id);
                                    if (unlockData != null && !EntryUnlockData.DEFAULT.equals(unlockData)) {
                                        data.entryUnlockData.put(id, unlockData);
                                    }
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

    private static EntryUnlockData parseUnlockData(JsonObject obj) {
        if (!obj.has("unlock")) return EntryUnlockData.DEFAULT;
        JsonObject unlock = obj.getAsJsonObject("unlock");

        boolean unlockedByDefault = GsonHelper.getAsBoolean(unlock, "unlocked_by_default", false);
        List<ResourceLocation> prerequisites = new ArrayList<>();
        if (unlock.has("prerequisites")) {
            for (JsonElement e : unlock.getAsJsonArray("prerequisites")) {
                prerequisites.add(ResourceLocation.parse(e.getAsString()));
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
                    triggerOn.add(ResourceLocation.parse(el.getAsString()));
                }
            } else {
                triggerOn.add(ResourceLocation.parse(e.getAsString()));
            }
        }

        return new EntryUnlockData(unlockedByDefault, prerequisites, triggers, triggerOn);
    }

    private static void loadComposites(ResourceManager resourceManager, ServerFieldGuideManager.ReloadData data) {
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
                            ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(obj, "id"));

                            if (GsonHelper.getAsBoolean(obj, "replace", false)) {
                                data.composites.removeIf(c -> c.id().equals(id));
                            }

                            ResourceLocation displayId = obj.has("display") ? ResourceLocation.parse(GsonHelper.getAsString(obj, "display")) : id;

                            List<ResourceLocation> components = new ArrayList<>();
                            if (obj.has("components")) {
                                for (JsonElement comp : GsonHelper.getAsJsonArray(obj, "components")) {
                                    components.add(ResourceLocation.parse(comp.getAsString()));
                                }
                            }

                            ResourceLocation structureNbt = obj.has("structure_nbt") ? ResourceLocation.parse(GsonHelper.getAsString(obj, "structure_nbt")) : null;

                            List<String> stackedBlocks = null;
                            if (obj.has("render")) {
                                stackedBlocks = new ArrayList<>();
                                for (JsonElement el2 : GsonHelper.getAsJsonArray(obj, "render")) {
                                    stackedBlocks.add(el2.getAsString());
                                }
                            }

                            List<EntryVariantData> visualVariants = null;
                            if (obj.has("visual_variants")) {
                                visualVariants = new ArrayList<>();
                                for (JsonElement vEl : GsonHelper.getAsJsonArray(obj, "visual_variants")) {
                                    JsonObject vObj = vEl.getAsJsonObject();
                                    String variantId = GsonHelper.getAsString(vObj, "variant_id");
                                    Component displayName = vObj.has("display_name") ? Component.literal(GsonHelper.getAsString(vObj, "display_name")) : Component.literal(variantId);

                                    EntryVariantData.DisplayType displayType;
                                    ResourceLocation variantDisplayId = null;
                                    CompoundTag variantNbt = null;
                                    if (vObj.has("entity")) {
                                        displayType = EntryVariantData.DisplayType.ENTITY;
                                        variantDisplayId = ResourceLocation.parse(GsonHelper.getAsString(vObj, "entity"));
                                        if (vObj.has("nbt")) {
                                            try {
                                                //? if <26.1 {
                                                variantNbt = TagParser.parseTag(GsonHelper.getAsString(vObj, "nbt"));
                                                //?} else {
                                                /*variantNbt = TagParser.parseCompoundFully(GsonHelper.getAsString(vObj, "nbt"));
                                                *///?}
                                            } catch (Exception e) {
                                                Constants.LOG.error("Invalid nbt for visual variant {} in composite {}: {}", variantId, id, e.getMessage());
                                            }
                                        }
                                    } else if (vObj.has("block")) {
                                        displayType = EntryVariantData.DisplayType.BLOCK;
                                        variantDisplayId = ResourceLocation.parse(GsonHelper.getAsString(vObj, "block"));
                                    } else if (vObj.has("item")) {
                                        displayType = EntryVariantData.DisplayType.ITEM;
                                        variantDisplayId = ResourceLocation.parse(GsonHelper.getAsString(vObj, "item"));
                                    } else {
                                        displayType = EntryVariantData.DisplayType.STRUCTURE;
                                    }

                                    ResourceLocation vStructureNbt = vObj.has("structure_nbt") ? ResourceLocation.parse(GsonHelper.getAsString(vObj, "structure_nbt")) : null;
                                    List<String> vRender = null;
                                    if (vObj.has("render")) {
                                        vRender = new ArrayList<>();
                                        for (JsonElement el2 : GsonHelper.getAsJsonArray(vObj, "render")) {
                                            vRender.add(el2.getAsString());
                                        }
                                    }

                                    StructureData vStructData = (displayType == EntryVariantData.DisplayType.STRUCTURE && (vStructureNbt != null || vRender != null))
                                            ? new StructureData(vStructureNbt, vRender) : null;

                                    List<String> vComponents = new ArrayList<>();
                                    if (vObj.has("components")) {
                                        for (JsonElement el2 : GsonHelper.getAsJsonArray(vObj, "components")) {
                                            vComponents.add(el2.getAsString());
                                        }
                                    } else if (vRender != null) {
                                        vComponents.addAll(vRender);
                                    } else if (variantDisplayId != null) {
                                        vComponents.add(variantDisplayId.toString());
                                    }

                                    visualVariants.add(new EntryVariantData(variantId, displayName, displayType, vStructData, variantDisplayId, variantNbt, null, vComponents));
                                }
                            }

                            data.composites.add(new CompositeDefinition(id, displayId, components, structureNbt, stackedBlocks, visualVariants));
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load composite: {}", entry.getKey(), e);
                }
            }
        }
    }

    private static void loadRedirects(ResourceManager resourceManager, ServerFieldGuideManager.ReloadData data) {
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
                            ResourceLocation source = EntryResolver.getRawId(ResourceLocation.parse(GsonHelper.getAsString(obj, "source")));
                            ResourceLocation target = EntryResolver.getRawId(ResourceLocation.parse(GsonHelper.getAsString(obj, "target")));
                            data.redirects.put(source, target);
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load redirect: {}", entry.getKey(), e);
                }
            }
        }
    }

    private static void loadVariants(ResourceManager resourceManager, ServerFieldGuideManager.ReloadData data) {
        Map<ResourceLocation, List<Resource>> variantResources = resourceManager.listResourceStacks(
                "fieldguide/variants",
                id -> id.getPath().endsWith(".json")
        );

        // vanilla first so modded variants of the same entity are added after it
        List<Map.Entry<ResourceLocation, List<Resource>>> sortedVariantResources = new ArrayList<>(variantResources.entrySet());
        sortedVariantResources.sort(Comparator.comparing(e -> !e.getKey().getNamespace().equals("minecraft")));

        for (Map.Entry<ResourceLocation, List<Resource>> entry : sortedVariantResources) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    if (json.has("entries")) {
                        for (JsonElement el : GsonHelper.getAsJsonArray(json, "entries")) {
                            JsonObject obj = el.getAsJsonObject();

                            if (obj.has("conditions")) {
                                JsonArray conditions = GsonHelper.getAsJsonArray(obj, "conditions");
                                if (!VariantConditionEvaluator.evaluateAll(conditions)) {
                                    continue;
                                }
                            }

                            ResourceLocation entityId = ResourceLocation.parse(GsonHelper.getAsString(obj, "id"));

                            boolean replace = GsonHelper.getAsBoolean(obj, "replace", false);
                            List<DatapackVariant> variantList = new ArrayList<>();

                            if (obj.has("variants")) {
                                for (JsonElement vEl : GsonHelper.getAsJsonArray(obj, "variants")) {
                                    JsonObject vObj = vEl.getAsJsonObject();
                                    String id = GsonHelper.getAsString(vObj, "id");
                                    //? if <26.1 {
                                    CompoundTag nbt = TagParser.parseTag(GsonHelper.getAsString(vObj, "nbt"));
                                    //?} else {
                                    /*CompoundTag nbt = TagParser.parseCompoundFully(GsonHelper.getAsString(vObj, "nbt"));
                                    *///?}
                                    variantList.add(new DatapackVariant(id, nbt));
                                }
                            }
                            DatapackVariantDefinition existing = data.variants.get(entityId);
                            if (existing != null && !replace) {
                                List<DatapackVariant> merged = new ArrayList<>(existing.variants());
                                merged.addAll(variantList);
                                data.variants.put(entityId, new DatapackVariantDefinition(existing.replace(), merged));
                            } else {
                                data.variants.put(entityId, new DatapackVariantDefinition(replace, variantList));
                            }
                        }
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to load variant: {}", entry.getKey(), e);
                }
            }
        }
    }

    private static void loadModifiers(ResourceManager resourceManager, String path, List<String> additions, List<String> removals) {
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

    private static List<String> getAsList(JsonObject obj, String singular, String plural) {
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
}

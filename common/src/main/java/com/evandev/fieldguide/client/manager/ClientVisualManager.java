package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.data.EntryVisual;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ClientVisualManager {
    private static final ClientVisualManager INSTANCE = new ClientVisualManager();

    private final Map<ResourceLocation, EntryVisual> entryVisuals = new HashMap<>();

    private ClientVisualManager() {
    }

    public static ClientVisualManager getInstance() {
        return INSTANCE;
    }

    public EntryVisual getEntryVisual(String entryKey) {
        if (entryKey == null || entryKey.isEmpty()) return new EntryVisual();

        // 1. Try prefixed key (e.g. "entity:minecraft/chicken")
        ResourceLocation prefixedId = new ResourceLocation(entryKey);
        if (entryVisuals.containsKey(prefixedId)) {
            return entryVisuals.get(prefixedId);
        }

        // 2. Try base ID (e.g. "minecraft:chicken")
        if (entryKey.contains(":")) {
            String baseIdStr = entryKey.substring(entryKey.indexOf(":") + 1);
            if (baseIdStr.contains("/")) {
                String[] parts = baseIdStr.split("/", 2);
                ResourceLocation baseId = new ResourceLocation(parts[0], parts[1]);
                if (entryVisuals.containsKey(baseId)) {
                    return entryVisuals.get(baseId);
                }
            }

            ResourceLocation baseId = new ResourceLocation(baseIdStr);
            if (entryVisuals.containsKey(baseId)) {
                return entryVisuals.get(baseId);
            }
        }

        return new EntryVisual();
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        entryVisuals.clear();
        EntryRenderHelper.clearCache();

        loadVisuals(resourceManager, (derivedId, json) -> {
            ResourceLocation targetId = json.has("id") ? new ResourceLocation(GsonHelper.getAsString(json, "id")) : derivedId;
            EntryVisual visual = new EntryVisual();
            if (json.has("custom_sound"))
                visual.customSound = new ResourceLocation(GsonHelper.getAsString(json, "custom_sound"));
            if (json.has("alignment_icon")) {
                String iconValue = GsonHelper.getAsString(json, "alignment_icon");
                visual.alignmentIcon = switch (iconValue) {
                    case "neutral" -> Constants.NEUTRAL_ICON;
                    case "hostile" -> Constants.HOSTILE_ICON;
                    case "passive" -> Constants.PASSIVE_ICON;
                    default -> new ResourceLocation(iconValue);
                };
            }
            if (json.has("scale")) visual.scale = GsonHelper.getAsFloat(json, "scale");
            if (json.has("y_offset")) visual.yOffset = GsonHelper.getAsFloat(json, "y_offset");
            if (json.has("x_offset")) visual.xOffset = GsonHelper.getAsFloat(json, "x_offset");
            if (json.has("grid_scale")) visual.gridScale = GsonHelper.getAsFloat(json, "grid_scale");
            if (json.has("grid_y_offset")) visual.gridYOffset = GsonHelper.getAsFloat(json, "grid_y_offset");
            if (json.has("grid_x_offset")) visual.gridXOffset = GsonHelper.getAsFloat(json, "grid_x_offset");
            if (json.has("page_scale")) visual.pageScale = GsonHelper.getAsFloat(json, "page_scale");
            if (json.has("page_y_offset")) visual.pageYOffset = GsonHelper.getAsFloat(json, "page_y_offset");
            if (json.has("page_x_offset")) visual.pageXOffset = GsonHelper.getAsFloat(json, "page_x_offset");
            if (json.has("spawn_biomes")) {
                visual.spawnBiomes = new ArrayList<>();
                GsonHelper.getAsJsonArray(json, "spawn_biomes").forEach(el -> visual.spawnBiomes.add(new ResourceLocation(el.getAsString())));
            }
            entryVisuals.put(targetId, visual);
        });
    }

    private void loadVisuals(ResourceManager mgr, BiConsumer<ResourceLocation, JsonObject> processor) {
        mgr.listResourceStacks("fieldguide/entries", id -> id.getPath().endsWith(".json")).forEach((fileId, resources) -> {
            String path = fileId.getPath();
            String idPath = path.substring(("fieldguide/entries" + "/").length(), path.length() - ".json".length());
            ResourceLocation targetId = new ResourceLocation(fileId.getNamespace(), idPath);
            resources.forEach(resource -> {
                try (Reader reader = resource.openAsReader()) {
                    processor.accept(targetId, GsonHelper.parse(reader));
                } catch (Exception e) {
                    Constants.LOG.error("Error loading entry visuals: {}", fileId, e);
                }
            });
        });
    }

    public Map<ResourceLocation, EntryVisual> getEntryVisuals() {
        return entryVisuals;
    }
}

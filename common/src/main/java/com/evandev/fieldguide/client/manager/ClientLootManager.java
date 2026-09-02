package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.RequestLootPacket;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ClientLootManager {
    private static final ClientLootManager INSTANCE = new ClientLootManager();

    private final Map<Object, List<ItemStack>> dropCache = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> dropIndex = new HashMap<>();
    private final Set<ResourceLocation> requestedIds = new HashSet<>();

    private ClientLootManager() {
    }

    public static ClientLootManager getInstance() {
        return INSTANCE;
    }

    public void clear() {
        this.dropCache.clear();
        this.dropIndex.clear();
        this.requestedIds.clear();
    }

    public void updateLootCache(Map<ResourceLocation, List<ItemStack>> lootCache, boolean clearCache) {
        if (clearCache) {
            this.clear();
        }

        if (lootCache != null) {
            for (Map.Entry<ResourceLocation, List<ItemStack>> entry : lootCache.entrySet()) {
                ResourceLocation id = entry.getKey();
                List<ItemStack> drops = entry.getValue();
                dropCache.put(id, drops);

                for (ItemStack stack : drops) {
                    String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                    dropIndex.computeIfAbsent(name, k -> new HashSet<>()).add(id);
                }
            }
            ClientCacheManager.saveAllDrops(lootCache);
        }
    }

    public List<Object> getEntriesDropping(String dropName, boolean exact) {
        Set<ResourceLocation> matchedIds = new HashSet<>();
        String query = dropName.toLowerCase(Locale.ROOT);

        if (exact) {
            Set<ResourceLocation> ids = dropIndex.get(query);
            if (ids != null) matchedIds.addAll(ids);
        } else {
            for (Map.Entry<String, Set<ResourceLocation>> entry : dropIndex.entrySet()) {
                if (entry.getKey().contains(query)) {
                    matchedIds.addAll(entry.getValue());
                }
            }
        }

        Set<Object> uniqueResults = new LinkedHashSet<>();
        ClientCategoryManager categoryManager = ClientCategoryManager.getInstance();
        for (ResourceLocation id : matchedIds) {
            uniqueResults.addAll(categoryManager.getEntriesForTarget(id));
        }
        return new ArrayList<>(uniqueResults);
    }

    public void requestLoot(ResourceLocation entryId) {
        if (entryId != null && !requestedIds.contains(entryId)) {
            List<ItemStack> diskDrops = ClientCacheManager.loadDrops(entryId);
            if (diskDrops != null) {
                dropCache.put(entryId, diskDrops);
                return;
            }
            requestedIds.add(entryId);
            Services.NETWORK.sendToServer(new RequestLootPacket(entryId));
        }
    }

    public List<ItemStack> getDrops(Object entry) {
        return getDrops(entry, null);
    }

    public List<ItemStack> getDrops(Object entry, String variantId) {
        List<ItemStack> rawDrops = new ArrayList<>();
        if (entry instanceof GuideEntry ge && ge.isComposite()) {
            Set<Object> uniqueComponents = new HashSet<>();
            if (ge.displayId() != null) {
                uniqueComponents.addAll(EntryResolver.resolveAllRegistryObjects(ge.displayId()));
            }
            if (ge.childEntries() != null) {
                for (ResourceLocation compId : ge.childEntries()) {
                    uniqueComponents.addAll(EntryResolver.resolveAllRegistryObjects(compId));
                }
            }
            for (Object comp : uniqueComponents) {
                ResourceLocation id = AutoPopulateRegistry.getEntryId(comp, true);
                if (id != null) {
                    if (dropCache.containsKey(id)) {
                        rawDrops.addAll(dropCache.get(id));
                    } else {
                        requestLoot(id);
                    }
                }
            }
        } else {
            ResourceLocation id = AutoPopulateRegistry.getEntryId(entry, true);
            if (id != null) {
                if (dropCache.containsKey(id)) {
                    rawDrops.addAll(dropCache.get(id));
                } else {
                    requestLoot(id);
                }
            }
        }

        if (Services.PLATFORM.isModLoaded("cobblemon")) {
            rawDrops.addAll(ClientFieldGuideCobblemonCompat.getCobblemonDrops(entry));
        }

        List<ItemStack> distinct = new ArrayList<>();
        for (ItemStack stack : rawDrops) {
            if (distinct.stream().noneMatch(s -> isSameLootItem(s, stack))) {
                distinct.add(stack);
            }
        }
        return distinct;
    }

    private boolean isSameLootItem(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItem(a, b)) return false;
        if (a.getTag() == b.getTag()) return true;
        if (a.getTag() == null || b.getTag() == null) return false;

        CompoundTag tagA = a.getTag();
        CompoundTag tagB = b.getTag();

        Set<String> allKeys = new HashSet<>(tagA.getAllKeys());
        allKeys.addAll(tagB.getAllKeys());

        for (String key : allKeys) {
            if (key.equals("FieldGuideDropChance") || key.equals("FieldGuideMin") || key.equals("FieldGuideMax")) {
                continue;
            }

            Tag valA = tagA.get(key);
            Tag valB = tagB.get(key);

            if (valA == null) {
                if (valB != null) return false;
            } else if (!valA.equals(valB)) {
                return false;
            }
        }

        return true;
    }

}

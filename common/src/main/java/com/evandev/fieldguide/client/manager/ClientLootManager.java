package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.RequestLootPacket;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
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
        if (entry instanceof GuideEntry ge && ge.hasVisualVariants() && variantId != null && !variantId.isEmpty()) {
            EntryVariantData variant = ge.visualVariants().stream()
                    .filter(v -> v.variantId().equals(variantId)).findFirst().orElse(null);
            if (variant != null) {
                Set<Object> targets = new HashSet<>();
                addLootTarget(targets, variant.displayId());
                for (String comp : variant.components()) {
                    int pipe = comp.indexOf('|');
                    addLootTarget(targets, ResourceLocation.tryParse(pipe >= 0 ? comp.substring(0, pipe) : comp));
                }
                for (Object comp : targets) {
                    ResourceLocation id = AutoPopulateRegistry.getEntryId(comp, true);
                    if (id != null) {
                        if (dropCache.containsKey(id)) rawDrops.addAll(dropCache.get(id));
                        else requestLoot(id);
                    }
                }
                return distinctLoot(rawDrops);
            }
        }
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

    private void addLootTarget(Set<Object> targets, ResourceLocation id) {
        targets.addAll(EntryResolver.resolveAllRegistryObjects(id));
    }

    private List<ItemStack> distinctLoot(List<ItemStack> rawDrops) {
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

        ItemStack copyA = a.copy();
        ItemStack copyB = b.copy();

        copyA.remove(ModDataComponents.DROP_CHANCE.get());
        copyA.remove(ModDataComponents.MIN_DROP.get());
        copyA.remove(ModDataComponents.MAX_DROP.get());

        copyB.remove(ModDataComponents.DROP_CHANCE.get());
        copyB.remove(ModDataComponents.MIN_DROP.get());
        copyB.remove(ModDataComponents.MAX_DROP.get());

        return ItemStack.isSameItemSameComponents(copyA, copyB);
    }

}

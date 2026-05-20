package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.itemdescriptions.ItemDescriptionsCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ClientTextManager {
    private static final ClientTextManager INSTANCE = new ClientTextManager();

    private ClientTextManager() {
    }

    public static ClientTextManager getInstance() {
        return INSTANCE;
    }

    public String getLockedHint(Object entry) {
        return getLockedHint(entry, null);
    }

    public String getLockedHint(Object entry, String variantId) {
        ResourceLocation id = EntryResolver.getEntryId(entry, false);
        ResourceLocation prefixedId = EntryResolver.getEntryId(entry, true);
        if (id == null || prefixedId == null) return I18n.get("fieldguide.hint.default");

        String entryType = prefixedId.getNamespace();
        String path = prefixedId.getPath().replace('/', '.');

        // 1. custom variant hint
        if (variantId != null && !variantId.isEmpty()) {
            String variantStr = variantId.toLowerCase(java.util.Locale.ROOT);

            String newVariantKey = "fieldguide." + entryType + "." + path + "." + variantStr + ".hint";
            if (I18n.exists(newVariantKey)) return I18n.get(newVariantKey);

            String oldVariantKey = "fieldguide." + id.getNamespace() + "." + id.getPath() + "." + variantStr + ".hint";
            if (I18n.exists(oldVariantKey)) return I18n.get(oldVariantKey);
        }

        // 2. custom base hint
        String newOverrideKey = "fieldguide." + entryType + "." + path + ".hint";
        if (I18n.exists(newOverrideKey)) return I18n.get(newOverrideKey);

        String oldOverrideKey = "fieldguide." + id.getNamespace() + "." + id.getPath() + ".hint";
        if (I18n.exists(oldOverrideKey)) return I18n.get(oldOverrideKey);

        // 3. defaults based on known triggers
        ProgressManager progress = ProgressManager.getInstance();
        if (progress.isKillToUnlock(prefixedId) || progress.hasTrigger(prefixedId, "KILL")) {
            return I18n.get("fieldguide.hint.kill");
        }
        if (progress.isEatToUnlock(prefixedId) || progress.hasTrigger(prefixedId, "EAT")) {
            return I18n.get("fieldguide.hint.eat");
        }
        if (progress.hasTrigger(prefixedId, "OBTAIN")) {
            return I18n.get("fieldguide.hint.obtain");
        }

        // fallback default
        if (ServerConfig.get().enableNakedEyeScanning) {
            return I18n.get("fieldguide.hint.no_spyglass");
        }
        return I18n.get("fieldguide.hint.default");
    }

    public String getEntryDescription(Object entry) {
        return getEntryDescription(entry, null);
    }

    public String getEntryDescription(Object entry, String variantId) {
        ResourceLocation id = EntryResolver.getEntryId(entry, false);
        ResourceLocation prefixedId = EntryResolver.getEntryId(entry, true);
        if (id == null || prefixedId == null) return "";

        String custom = ProgressManager.getInstance().getCustomDescription(entry, variantId);
        if (custom != null) return custom;

        if (id.getNamespace().equals("fieldguide") && id.getPath().startsWith("cobblemon/")) {
            String species = id.getPath().substring("cobblemon/".length());
            int underscore = species.lastIndexOf('_');
            if (underscore != -1) species = species.substring(0, underscore);

            String descKey = "cobblemon.species." + species + ".desc";
            if (I18n.exists(descKey)) return I18n.get(descKey);
        }

        String entryType = prefixedId.getNamespace();
        String path = prefixedId.getPath().replace('/', '.');

        if (variantId != null && !variantId.isEmpty()) {
            String variantStr = variantId.toLowerCase(java.util.Locale.ROOT);

            String newVariantKey = "fieldguide." + entryType + "." + path + "." + variantStr + ".description";
            if (I18n.exists(newVariantKey)) return I18n.get(newVariantKey);

            String oldVariantKey = "fieldguide." + id.getNamespace() + "." + id.getPath() + "." + variantStr + ".description";
            if (I18n.exists(oldVariantKey)) return I18n.get(oldVariantKey);
        }

        String newOverrideKey = "fieldguide." + entryType + "." + path + ".description";
        if (I18n.exists(newOverrideKey)) return I18n.get(newOverrideKey);

        String oldOverrideKey = "fieldguide." + id.getNamespace() + "." + id.getPath() + ".description";
        if (I18n.exists(oldOverrideKey)) return I18n.get(oldOverrideKey);

        Object coreEntry = EntryResolver.resolveCoreEntry(entry);

        // Item Descriptions Compat
        if (Services.PLATFORM.isModLoaded("item_descriptions")) {
            String compatKey = ItemDescriptionsCompat.tryGetDescriptionKey(coreEntry);
            if (compatKey != null && I18n.exists(compatKey)) {
                return I18n.get(compatKey);
            }
        }

        // Entity Descriptions Compat
        String entityKey = "entity." + id.getNamespace() + "." + id.getPath() + ".description";
        if (coreEntry instanceof EntityType) {
            if (I18n.exists(entityKey)) return I18n.get(entityKey);
        }

        // Quark JEI Hint
        String quarkJeiKey = "quark.jei.hint." + id.getPath();
        if (id.getNamespace().equals("quark") && I18n.exists(quarkJeiKey)) {
            return I18n.get(quarkJeiKey);
        }

        // Lore & Item Fallbacks
        String loreKey = "lore." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(loreKey)) return I18n.get(loreKey);

        String fallbackKey = (coreEntry instanceof EntityType) ? entityKey : loreKey;
        if (coreEntry instanceof Item) {
            fallbackKey = "item." + id.getNamespace() + "." + id.getPath() + ".description";
        }
        return I18n.exists(fallbackKey) ? I18n.get(fallbackKey) : I18n.get("fieldguide.description.missing");
    }

    public Component getDefaultNameComponent(Object entry, String variantId) {
        ResourceLocation id = EntryResolver.getEntryId(entry, false);
        ResourceLocation prefixedId = EntryResolver.getEntryId(entry, true);

        if (id != null && prefixedId != null) {
            if (id.getNamespace().equals("fieldguide") && id.getPath().startsWith("cobblemon/")) {
                String species = id.getPath().substring("cobblemon/".length());
                int underscore = species.lastIndexOf('_');
                if (underscore != -1) species = species.substring(0, underscore);

                String nameKey = "cobblemon.species." + species + ".name";
                if (I18n.exists(nameKey)) {
                    return Component.translatable(nameKey);
                }
            }

            String entryType = prefixedId.getNamespace();
            String path = prefixedId.getPath().replace('/', '.');

            if (variantId != null && !variantId.isEmpty()) {
                String variantStr = variantId.toLowerCase(java.util.Locale.ROOT);

                String newVariantKey = "fieldguide.name." + entryType + "." + path + "." + variantStr;
                if (I18n.exists(newVariantKey)) return Component.translatable(newVariantKey);

                String oldVariantKey = "fieldguide.name." + id.getNamespace() + "." + id.getPath() + "." + variantStr;
                if (I18n.exists(oldVariantKey)) return Component.translatable(oldVariantKey);
            }

            String newOverrideKey = "fieldguide.name." + entryType + "." + path;
            if (I18n.exists(newOverrideKey)) return Component.translatable(newOverrideKey);

            String oldOverrideKey = "fieldguide.name." + id.getNamespace() + "." + id.getPath();
            if (I18n.exists(oldOverrideKey)) return Component.translatable(oldOverrideKey);
        }

        Object coreEntry = EntryResolver.resolveCoreEntry(entry);

        if (entry instanceof GuideEntry ge && ge.isStructure() && id != null && id.getPath().endsWith("_tree")) {
            if (coreEntry instanceof Block block) {
                String saplingName = block.getName().getString();
                return Component.literal(saplingName.replace("Sapling", "Tree"));
            }
        }

        if (coreEntry instanceof EntityType<?> type) return type.getDescription();
        if (coreEntry instanceof Block block) return block.getName();
        if (coreEntry instanceof Item item) return item.getDescription();

        return Component.translatable("fieldguide.unknown");
    }

    public Component getDefaultNameComponent(Object entry) {
        return getDefaultNameComponent(entry, null);
    }

    public Component getEntryName(Object entry, String variantId) {
        ResourceLocation id = EntryResolver.getEntryId(entry);
        String key = id != null ? id.toString() : entry.toString();
        if (variantId != null) key += "#" + variantId;
        String custom = ProgressManager.getInstance().getCustomName(key);
        if (custom != null) return Component.literal(custom);

        return getDefaultNameComponent(entry, variantId);
    }

    public void setCustomDescription(Object entry, String desc) {
        ProgressManager.getInstance().setCustomDescription(entry, null, desc);
    }

    public void setCustomDescription(Object entry, String variantId, String desc) {
        ProgressManager.getInstance().setCustomDescription(entry, variantId, desc);
    }

    public void setCustomName(Object entry, String name) {
        ProgressManager.getInstance().setCustomName(entry, name);
    }

    public Component getEntryName(Object entry) {
        return getEntryName(entry, null);
    }

    public String getDefaultName(Object entry) {
        return getDefaultNameComponent(entry).getString();
    }

    public String getJournalTitle() {
        return ProgressManager.getInstance().getJournalTitle();
    }

    public void setJournalTitle(String title) {
        ProgressManager.getInstance().setJournalTitle(title);
    }
}
package com.evandev.fieldguide.compat.cobblemon;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FieldGuideCobblemonCompat {
    public static final String MOD_ID = "cobblemon";
    private static final String POKEMON_PATH = "pokemon";

    private FieldGuideCobblemonCompat() {
    }

    public static void init() {
        if (isLoaded()) {
            CobblemonCompatImpl.register();
        }
    }

    public static boolean isLoaded() {
        return Services.PLATFORM.isModLoaded(MOD_ID);
    }

    public static boolean looksStandardForm(String name) {
        if (name == null || name.isBlank()) return true;
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("standard") || normalized.equals("default") || normalized.equals("normal") || normalized.equals("base");
    }

    public static boolean isCobblemonId(ResourceLocation id) {
        if (id == null || !isLoaded()) return false;
        return (Constants.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("cobblemon/"))
                || MOD_ID.equals(id.getNamespace());
    }

    public static boolean isCobblemonEntry(Object entry) {
        if (entry == null || !isLoaded()) return false;
        switch (entry) {
            case GuideEntry ge -> {
                if (ge.isVirtual() && ge.virtualData() != null && MOD_ID.equals(ge.virtualData().virtualType())) {
                    return true;
                }
                return isCobblemonId(ge.id());
            }
            case ResourceLocation id -> {
                return isCobblemonId(id);
            }
            case Entity entity -> {
                return isPokemon(entity);
            }
            default -> {
            }
        }
        return false;
    }

    public static String getSpeciesTranslationKey(ResourceLocation id) {
        String species = getSpeciesName(id);
        return "cobblemon.species." + species + ".name";
    }

    public static String getSpeciesDescriptionKey(ResourceLocation id) {
        String species = getSpeciesName(id);
        return "cobblemon.species." + species + ".desc";
    }

    public static String getCurrentForm(LivingEntity entity) {
        if (!isLoaded()) return "standard";
        return CobblemonCompatImpl.getCurrentForm(entity);
    }

    public static String getDefaultForm(ResourceLocation id) {
        String path = id.getPath();
        int idx = path.lastIndexOf("cobblemon/");
        String speciesAndForm = idx != -1 ? path.substring(idx + "cobblemon/".length()) : path;

        int underscoreIndex = speciesAndForm.lastIndexOf('_');
        if (underscoreIndex != -1) {
            return speciesAndForm.substring(underscoreIndex + 1);
        }
        return "standard";
    }

    public static String getSpeciesName(ResourceLocation id) {
        String path = id.getPath();
        if (!path.startsWith("cobblemon/")) return path;
        String speciesAndForm = path.substring("cobblemon/".length());
        int underscoreIndex = speciesAndForm.lastIndexOf('_');
        if (underscoreIndex != -1) {
            return speciesAndForm.substring(0, underscoreIndex);
        }
        return speciesAndForm;
    }

    public static List<String> getVariantIds(ResourceLocation entryId) {
        if (!isLoaded()) return List.of();
        return CobblemonCompatImpl.getVariantIds(entryId);
    }

    public static ResourceLocation getPokemonEntryId(Entity entity) {
        if (!isLoaded()) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }
        return CobblemonCompatImpl.getPokemonEntryId(entity);
    }

    public static void populateCache(ResourceManager resourceManager) {
        if (isLoaded()) {
            CobblemonCompatImpl.populateCache(resourceManager);
        }
    }

    public static List<Object> getAutoPopulateEntries() {
        if (!isLoaded()) return List.of();
        return CobblemonCompatImpl.getAutoPopulateEntries();
    }

    public static boolean isPokemon(Entity entity) {
        if (entity == null || !isLoaded()) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return MOD_ID.equals(id.getNamespace()) && POKEMON_PATH.equals(id.getPath());
    }

    public static boolean isPokemonType(EntityType<?> type) {
        if (type == null || !isLoaded()) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return MOD_ID.equals(id.getNamespace());
    }

    public static Set<String> getCobblemonBiomesForId(ResourceLocation id) {
        if (!isLoaded()) return null;
        return CobblemonCompatImpl.getCobblemonBiomesForId(id);
    }

    public static List<ItemStack> getCobblemonDropsForId(ResourceLocation id) {
        if (!isLoaded()) return null;
        return CobblemonCompatImpl.getCobblemonDropsForId(id);
    }
}
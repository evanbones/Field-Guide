package com.evandev.fieldguide.compat.cobblemon;

//? if <26.1 {
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.progress.ProgressManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.*;

public final class ClientFieldGuideCobblemonCompat {

    private static final Map<ResourceLocation, LivingEntity> DUMMY_CACHE = new HashMap<>();
    private static final Map<String, LivingEntity> VARIANT_DUMMY_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, String> FORM_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> RESOLVED_BIOME_CACHE = new HashMap<>();

    private ClientFieldGuideCobblemonCompat() {
    }

    public static void clearCache() {
        DUMMY_CACHE.clear();
        VARIANT_DUMMY_CACHE.clear();
        RESOLVED_BIOME_CACHE.clear();
    }

    public static String getFormForEntry(ResourceLocation id) {
        String selected = ProgressManager.getInstance().getSelectedVariant(id);
        if (selected != null) return selected;
        return FORM_CACHE.getOrDefault(id, FieldGuideCobblemonCompat.getDefaultForm(id));
    }

    public static LivingEntity getDummyVariant(ResourceLocation id, String variantName, Level level) {
        String cacheKey = id.toString() + "#" + variantName;
        if (VARIANT_DUMMY_CACHE.containsKey(cacheKey)) {
            return VARIANT_DUMMY_CACHE.get(cacheKey);
        }

        String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);

        try {
            PokemonProperties props = PokemonProperties.Companion.parse("species=" + speciesName + " form=" + variantName, " ", "=");
            PokemonEntity pokemonEntity = props.createEntity(level);
            pokemonEntity.setNoAi(true);

            Pokemon pokemon = pokemonEntity.getPokemon();
            FormData form = pokemon.getSpecies().getForms().stream()
                    .filter(f -> f.getName().equalsIgnoreCase(variantName))
                    .findFirst()
                    .orElse(null);
            if (form == null && FieldGuideCobblemonCompat.looksStandardForm(variantName)) {
                form = pokemon.getSpecies().getStandardForm();
            }
            if (form != null) {
                pokemon.setForm(form);
                Set<String> aspects = new HashSet<>(form.getAspects());
                if (pokemon.getShiny()) aspects.add("shiny");
                pokemon.setForcedAspects(aspects);
            }
            pokemon.updateAspects();

            pokemonEntity.getEntityData().set(PokemonEntity.getASPECTS(), pokemon.getAspects());
            pokemonEntity.onSyncedDataUpdated(PokemonEntity.getASPECTS());
            pokemonEntity.getEntityData().set(PokemonEntity.getSPECIES(), pokemon.getSpecies().getResourceIdentifier().toString());
            pokemonEntity.onSyncedDataUpdated(PokemonEntity.getSPECIES());

            pokemonEntity.setTicksLived(25);
            pokemonEntity.setYRot(0.0F);
            pokemonEntity.yRotO = 0.0F;
            pokemonEntity.setXRot(0.0F);
            pokemonEntity.xRotO = 0.0F;
            pokemonEntity.setYHeadRot(0.0F);
            pokemonEntity.yHeadRot = 0.0F;
            pokemonEntity.yHeadRotO = 0.0F;
            pokemonEntity.setYBodyRot(0.0F);
            pokemonEntity.yBodyRot = 0.0F;
            pokemonEntity.yBodyRotO = 0.0F;

            VARIANT_DUMMY_CACHE.put(cacheKey, pokemonEntity);
            return pokemonEntity;
        } catch (Exception e) {
            Constants.LOG.error("Failed to construct Cobblemon dummy variant for Field Guide ID: {} variant: {}", id, variantName, e);
        }

        return null;
    }

    public static LivingEntity getDummyPokemon(ResourceLocation id, Level level) {
        String selectedForm = ProgressManager.getInstance().getSelectedVariant(id);
        if (selectedForm != null) {
            return getDummyVariant(id, selectedForm, level);
        }

        if (DUMMY_CACHE.containsKey(id)) {
            return DUMMY_CACHE.get(id);
        }

        String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);
        String formName = FORM_CACHE.getOrDefault(id, FieldGuideCobblemonCompat.getDefaultForm(id));

        try {
            PokemonProperties props = PokemonProperties.Companion.parse("species=" + speciesName + " form=" + formName, " ", "=");
            PokemonEntity pokemonEntity = props.createEntity(level);
            pokemonEntity.setNoAi(true);

            Pokemon pokemon = pokemonEntity.getPokemon();
            FormData form = pokemon.getSpecies().getForms().stream()
                    .filter(f -> f.getName().equalsIgnoreCase(formName))
                    .findFirst()
                    .orElse(null);
            if (form == null && FieldGuideCobblemonCompat.looksStandardForm(formName)) {
                form = pokemon.getSpecies().getStandardForm();
            }
            if (form != null) {
                pokemon.setForm(form);
                Set<String> aspects = new HashSet<>(form.getAspects());
                if (pokemon.getShiny()) aspects.add("shiny");
                pokemon.setForcedAspects(aspects);
            }
            pokemon.updateAspects();

            pokemonEntity.getEntityData().set(PokemonEntity.getASPECTS(), pokemon.getAspects());
            pokemonEntity.onSyncedDataUpdated(PokemonEntity.getASPECTS());
            pokemonEntity.getEntityData().set(PokemonEntity.getSPECIES(), pokemon.getSpecies().getResourceIdentifier().toString());

            pokemonEntity.setTicksLived(25);
            pokemonEntity.setYRot(0.0F);
            pokemonEntity.yRotO = 0.0F;
            pokemonEntity.setXRot(0.0F);
            pokemonEntity.xRotO = 0.0F;
            pokemonEntity.setYHeadRot(0.0F);
            pokemonEntity.yHeadRot = 0.0F;
            pokemonEntity.yHeadRotO = 0.0F;
            pokemonEntity.setYBodyRot(0.0F);
            pokemonEntity.yBodyRot = 0.0F;
            pokemonEntity.yBodyRotO = 0.0F;

            DUMMY_CACHE.put(id, pokemonEntity);
            return pokemonEntity;
        } catch (Exception e) {
            Constants.LOG.error("Failed to construct Cobblemon dummy entity for Field Guide ID: {}", id, e);
        }

        return null;
    }

    public static void playPokemonCry(Entity entity) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            String speciesName = pokemonEntity.getPokemon().getSpecies().getResourceIdentifier().getPath();
            ResourceLocation cryId = ResourceLocation.fromNamespaceAndPath(FieldGuideCobblemonCompat.MOD_ID, "pokemon." + speciesName + ".cry");
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(cryId), 1.0F, 1.0F));
        }
    }

    public static List<ItemStack> getCobblemonDrops(Object entry) {
        ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
        if (id == null) return List.of();

        List<ItemStack> drops = FieldGuideCobblemonCompat.getCobblemonDropsForId(id);
        if (drops != null) {
            return drops;
        }

        String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);
        ResourceLocation standardId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");
        drops = FieldGuideCobblemonCompat.getCobblemonDropsForId(standardId);

        if (drops != null) {
            return drops;
        }

        return List.of();
    }

    public static List<ResourceLocation> getCobblemonBiomes(Object entry) {
        ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
        if (id == null) return List.of();

        if (RESOLVED_BIOME_CACHE.containsKey(id)) {
            return RESOLVED_BIOME_CACHE.get(id);
        }

        Set<String> conditions = FieldGuideCobblemonCompat.getCobblemonBiomesForId(id);
        if (conditions == null) {
            String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);
            ResourceLocation standardId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");
            conditions = FieldGuideCobblemonCompat.getCobblemonBiomesForId(standardId);
        }

        if (conditions == null) return List.of();

        List<ResourceLocation> results = new ArrayList<>();
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var biomeRegistry = connection.registryAccess().registryOrThrow(Registries.BIOME);
            for (var biomeEntry : biomeRegistry.entrySet()) {
                if (isCobblemonBiomeMatch(entry, biomeEntry.getKey().location(), biomeRegistry.getHolderOrThrow(biomeEntry.getKey()))) {
                    results.add(biomeEntry.getKey().location());
                }
            }
        }
        RESOLVED_BIOME_CACHE.put(id, results);
        return results;
    }

    public static boolean isCobblemonBiomeMatch(Object entry, ResourceLocation biomeId, Holder<Biome> holder) {
        ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
        if (id == null) return false;

        Set<String> conditions = FieldGuideCobblemonCompat.getCobblemonBiomesForId(id);
        if (conditions == null) {
            String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);
            ResourceLocation standardId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");
            conditions = FieldGuideCobblemonCompat.getCobblemonBiomesForId(standardId);
        }

        if (conditions == null) return false;

        for (String condition : conditions) {
            if (condition.startsWith("#")) {
                String tagPath = condition.substring(1);
                TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, ResourceLocation.parse(tagPath));
                if (holder.is(tagKey)) return true;
            } else {
                if (biomeId.toString().equals(condition) || biomeId.getPath().equals(condition)) return true;
            }
        }

        return false;
    }
}
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class ClientFieldGuideCobblemonCompat {

    public static void clearCache() {
    }

    public static String getFormForEntry(ResourceLocation id) {
        return "standard";
    }

    public static LivingEntity getDummyVariant(ResourceLocation id, String variantName, Level level) {
        return null;
    }

    public static LivingEntity getDummyPokemon(ResourceLocation id, Level level) {
        return null;
    }

    public static void playPokemonCry(Entity entity) {
    }

    public static List<ItemStack> getCobblemonDrops(Object entry) {
        return List.of();
    }

    public static List<ResourceLocation> getCobblemonBiomes(Object entry) {
        return List.of();
    }

    public static boolean isCobblemonBiomeMatch(Object entry, ResourceLocation biomeId, Holder<Biome> holder) {
        return false;
    }
}
*///?}

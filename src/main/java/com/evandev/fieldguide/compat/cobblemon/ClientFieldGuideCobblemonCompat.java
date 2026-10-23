package com.evandev.fieldguide.compat.cobblemon;

//? if <26.1 {
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.progress.ProgressManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.*;

public final class ClientFieldGuideCobblemonCompat {

    private static final Map<String, LivingEntity> VARIANT_DUMMY_CACHE = new HashMap<>();

    public static void clearCache() {
        VARIANT_DUMMY_CACHE.clear();
    }

    public static boolean isSpeciesAvailable(ResourceLocation id) {
        return CobblemonCompatImpl.getSpecies(id) != null;
    }

    public static String getFormForEntry(ResourceLocation id) {
        String selected = ProgressManager.getInstance().getSelectedVariant(id);
        if (selected != null) return selected;
        return FieldGuideCobblemonCompat.getDefaultForm(id);
    }

    public static LivingEntity getDummyVariant(ResourceLocation id, String variantName, Level level) {
        String cacheKey = id.toString() + "#" + variantName;
        if (VARIANT_DUMMY_CACHE.containsKey(cacheKey)) {
            return VARIANT_DUMMY_CACHE.get(cacheKey);
        }

        String speciesName = FieldGuideCobblemonCompat.getSpeciesName(id);

        try {
            PokemonProperties props = PokemonProperties.Companion.parse("species=" + speciesName + " form=" + variantName, " ", "=");
            Pokemon pokemon = new Pokemon();
            props.apply(pokemon);
            pokemon.checkGender();

            PokemonEntity pokemonEntity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
            pokemonEntity.setNoAi(true);

            CobblemonCompatImpl.applyForm(pokemon, variantName);
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
        return getDummyVariant(id, getFormForEntry(id), level);
    }

    public static void playPokemonCry(Entity entity) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            String speciesName = pokemonEntity.getPokemon().getSpecies().getResourceIdentifier().getPath();
            ResourceLocation cryId = ResourceLocation.fromNamespaceAndPath(FieldGuideCobblemonCompat.MOD_ID, "pokemon." + speciesName + ".cry");
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(cryId), 1.0F, 1.0F));
        }
    }
}
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ClientFieldGuideCobblemonCompat {

    public static void clearCache() {
    }

    public static boolean isSpeciesAvailable(ResourceLocation id) {
        return false;
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
}
*///?}

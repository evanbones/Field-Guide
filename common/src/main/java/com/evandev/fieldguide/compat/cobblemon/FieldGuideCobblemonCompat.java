package com.evandev.fieldguide.compat.cobblemon;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.api.EntryKind;
import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.VirtualData;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.Reader;
import java.util.*;

public final class FieldGuideCobblemonCompat {
    public static final String MOD_ID = "cobblemon";
    private static final String POKEMON_PATH = "pokemon";

    private static final Map<ResourceLocation, List<ItemStack>> COBBLEMON_DROPS_CACHE = new HashMap<>();
    private static final List<Object> AUTO_POPULATE_CACHE = new ArrayList<>();
    private static final Map<ResourceLocation, Set<String>> COBBLEMON_BIOMES_CACHE = new HashMap<>();

    static {
        FieldGuideVariantManager.registerProvider(PokemonEntity.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(PokemonEntity entity) {
                Species species = entity.getPokemon().getSpecies();
                Map<String, VariantDef> variantMap = new LinkedHashMap<>();

                String standardName = species.getStandardForm().getName();
                variantMap.put(standardName.toLowerCase(Locale.ROOT), new VariantDef(standardName, standardName));

                for (FormData form : species.getForms()) {
                    String name = form.getName();
                    String normalized = name.toLowerCase(Locale.ROOT);
                    if (looksStandardForm(normalized) || normalized.equals(standardName.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    variantMap.putIfAbsent(normalized, new VariantDef(name, name));
                }
                return new ArrayList<>(variantMap.values());
            }

            @Override
            public void apply(PokemonEntity entity, VariantDef def) {
                String formName = def.id();
                Pokemon pokemon = entity.getPokemon();
                FormData form = pokemon.getSpecies().getForms().stream()
                        .filter(f -> f.getName().equalsIgnoreCase(formName))
                        .findFirst()
                        .orElse(null);

                if (form == null && looksStandardForm(formName)) {
                    form = pokemon.getSpecies().getStandardForm();
                }

                if (form != null) {
                    pokemon.setForm(form);
                    Set<String> aspects = new HashSet<>(form.getAspects());
                    if (pokemon.getShiny()) aspects.add("shiny");
                    pokemon.setForcedAspects(aspects);

                    pokemon.updateAspects();
                    entity.refreshDimensions();

                    entity.getEntityData().set(PokemonEntity.getASPECTS(), pokemon.getAspects());
                    entity.onSyncedDataUpdated(PokemonEntity.getASPECTS());
                }
            }

            @Override
            public VariantDef getCurrent(PokemonEntity entity) {
                String formName = entity.getPokemon().getForm().getName();
                return new VariantDef(formName, formName);
            }

            @Override
            public String getCacheKey(PokemonEntity entity) {
                return MOD_ID + ":" + entity.getPokemon().getSpecies().getResourceIdentifier().getPath();
            }
        });
    }

    private FieldGuideCobblemonCompat() {
    }

    public static boolean looksStandardForm(String name) {
        if (name == null || name.isBlank()) return true;
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("standard") || normalized.equals("default") || normalized.equals("normal") || normalized.equals("base");
    }

    public static String getCurrentForm(LivingEntity entity) {
        if (entity instanceof PokemonEntity pokemon) {
            return pokemon.getPokemon().getForm().getName();
        }
        return "standard";
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
        String speciesName = getSpeciesName(entryId);
        Species species = PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath(MOD_ID, speciesName));
        if (species != null) {
            return species.getForms().stream().map(FormData::getName).toList();
        }
        return List.of();
    }

    public static ResourceLocation getPokemonEntryId(Entity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }

        try {
            Pokemon pokemon = pokemonEntity.getPokemon();
            Species species = pokemon.getSpecies();

            String speciesName = species.getResourceIdentifier().getPath();
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse Cobblemon properties for Field Guide ID", e);
        }

        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    public static void populateCache(ResourceManager resourceManager) {
        AUTO_POPULATE_CACHE.clear();
        COBBLEMON_DROPS_CACHE.clear();

        Map<ResourceLocation, List<Resource>> speciesFiles = resourceManager.listResourceStacks(
                "species", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json")
        );

        List<Map.Entry<GuideEntry, Integer>> sortedEntries = new ArrayList<>();

        for (Map.Entry<ResourceLocation, List<Resource>> entry : speciesFiles.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);

                    if (json.has("name")) {
                        String speciesName = json.get("name").getAsString().toLowerCase(Locale.ROOT);
                        ResourceLocation entryId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");
                        int pokedexNumber = json.has("nationalPokedexNumber") ? json.get("nationalPokedexNumber").getAsInt() : Integer.MAX_VALUE;

                        if (json.has("drops")) {
                            JsonObject dropsObj = json.getAsJsonObject("drops");
                            if (dropsObj.has("entries")) {
                                List<ItemStack> drops = new ArrayList<>();
                                for (JsonElement dropElem : dropsObj.getAsJsonArray("entries")) {
                                    JsonObject dropJson = dropElem.getAsJsonObject();
                                    if (dropJson.has("item")) {
                                        String itemStr = dropJson.get("item").getAsString();
                                        float chance = dropJson.has("percentage") ? dropJson.get("percentage").getAsFloat() : 100f;
                                        int quantity = dropJson.has("quantity") ? dropJson.get("quantity").getAsInt() : 1;

                                        Item item = BuiltInRegistries.ITEM.get(EntryResolver.getRawId(ResourceLocation.parse(itemStr)));
                                        if (item != Items.AIR) {
                                            ItemStack stack = new ItemStack(item, quantity);
                                            stack.set(ModDataComponents.DROP_CHANCE.get(), chance);
                                            drops.add(stack);
                                        }
                                    }
                                }
                                if (!drops.isEmpty()) {
                                    COBBLEMON_DROPS_CACHE.put(entryId, drops);
                                }
                            }
                        }

                        sortedEntries.add(new AbstractMap.SimpleEntry<>(
                                new GuideEntry(entryId,
                                        null,
                                        null,
                                        EntryKind.NORMAL,
                                        true,
                                        false,
                                        null,
                                        null,
                                        null,
                                        null,
                                        new VirtualData("cobblemon"),
                                        EntryUnlockData.DEFAULT),
                                pokedexNumber
                        ));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        sortedEntries.sort(Map.Entry.comparingByValue());
        for (Map.Entry<GuideEntry, Integer> sortedEntry : sortedEntries) {
            AUTO_POPULATE_CACHE.add(sortedEntry.getKey());
        }

        Map<ResourceLocation, List<Resource>> spawnPoolFiles = resourceManager.listResourceStacks(
                "spawn_pool_world", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json")
        );

        if (spawnPoolFiles.isEmpty()) {
            spawnPoolFiles = resourceManager.listResourceStacks(
                    "spawn_pool", id -> id.getNamespace().equals(MOD_ID) && id.getPath().endsWith(".json")
            );
        }

        for (Map.Entry<ResourceLocation, List<Resource>> entry : spawnPoolFiles.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = GsonHelper.parse(reader);
                    if (json.has("spawns")) {
                        JsonArray spawns = json.getAsJsonArray("spawns");
                        for (JsonElement spawnEl : spawns) {
                            JsonObject spawn = spawnEl.getAsJsonObject();
                            if (spawn.has("pokemon")) {
                                String pokemonStr = spawn.get("pokemon").getAsString().toLowerCase(Locale.ROOT);
                                String speciesName = pokemonStr.split(" ")[0];
                                ResourceLocation entryId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cobblemon/" + speciesName + "_standard");

                                if (spawn.has("condition")) {
                                    JsonObject condition = spawn.getAsJsonObject("condition");
                                    if (condition.has("biomes")) {
                                        JsonArray biomes = condition.getAsJsonArray("biomes");
                                        for (JsonElement biomeEl : biomes) {
                                            COBBLEMON_BIOMES_CACHE.computeIfAbsent(entryId, k -> new HashSet<>()).add(biomeEl.getAsString());
                                        }
                                    }
                                }
                                if (spawn.has("anticondition")) {
                                    JsonObject anti = spawn.getAsJsonObject("anticondition");
                                    if (anti.has("biomes")) {
                                        JsonArray biomes = anti.getAsJsonArray("biomes");
                                        for (JsonElement biomeEl : biomes) {
                                            Set<String> set = COBBLEMON_BIOMES_CACHE.get(entryId);
                                            if (set != null) set.remove(biomeEl.getAsString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static List<Object> getAutoPopulateEntries() {
        return AUTO_POPULATE_CACHE;
    }

    public static boolean isPokemon(Entity entity) {
        if (entity == null || !Services.PLATFORM.isModLoaded(MOD_ID)) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return MOD_ID.equals(id.getNamespace()) && POKEMON_PATH.equals(id.getPath());
    }

    public static boolean isPokemonType(EntityType<?> type) {
        if (!Services.PLATFORM.isModLoaded(MOD_ID)) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return MOD_ID.equals(id.getNamespace());
    }

    public static Set<String> getCobblemonBiomesForId(ResourceLocation id) {
        return COBBLEMON_BIOMES_CACHE.get(id);
    }

    public static List<ItemStack> getCobblemonDropsForId(ResourceLocation id) {
        return COBBLEMON_DROPS_CACHE.get(id);
    }
}
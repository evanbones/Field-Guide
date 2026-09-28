package com.evandev.fieldguide.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

//? if >=26.3 {
/*import net.minecraft.world.attribute.EnvironmentAttributes;
*///?}

public final class BiomeSpawns {
    private BiomeSpawns() {
    }

    public static List<EntityType<?>> types(Biome biome, MobCategory category) {
        //? if <26.1 {
        return biome.getMobSettings().getMobs(category).unwrap().stream().<EntityType<?>>map(spawn -> spawn.type).toList();
        //?} else if <26.3 {
        /*return biome.getMobSettings().getMobs(category).unwrap().stream().<EntityType<?>>map(spawn -> spawn.value().type()).toList();
        *///?} else {
        /*return biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, EnvironmentAttributes.NATURAL_MOB_SPAWNS.defaultValue())
                .getMobsToSpawn(category).unwrap().stream().<EntityType<?>>map(spawn -> spawn.value().type()).toList();
        *///?}
    }
}

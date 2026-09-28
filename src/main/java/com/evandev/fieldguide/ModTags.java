package com.evandev.fieldguide;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {
        public static final TagKey<Block> MULTIBLOCK_SCAN = tag("multiblock_scan");
        public static final TagKey<Block> PLANTS = tag("plants");
        public static final TagKey<Block> BLACKLISTED = tag("blacklisted");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> SPYGLASSES = tag("spyglasses");
        public static final TagKey<Item> LENSES = tag("lenses");
        public static final TagKey<Item> BLACKLISTED = tag("blacklisted");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
        }
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> BLACKLISTED = tag("blacklisted");
        public static final TagKey<EntityType<?>> HOSTILE = tag("hostile");
        public static final TagKey<EntityType<?>> NEUTRAL = tag("neutral");
        public static final TagKey<EntityType<?>> PASSIVE = tag("passive");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
        }
    }
}

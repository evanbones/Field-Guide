package com.evandev.fieldguide.compat.sereneseasons;

import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.seasons.SeasonsProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class SereneSeasonsProvider implements SeasonsProvider {
    private static final String MOD_ID = "sereneseasons";

    private static final TagKey<Block> SPRING_BLOCKS = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "spring_crops"));
    private static final TagKey<Block> SUMMER_BLOCKS = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "summer_crops"));
    private static final TagKey<Block> AUTUMN_BLOCKS = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "autumn_crops"));
    private static final TagKey<Block> WINTER_BLOCKS = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "winter_crops"));

    private static final TagKey<Item> SPRING_ITEMS = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "spring_crops"));
    private static final TagKey<Item> SUMMER_ITEMS = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "summer_crops"));
    private static final TagKey<Item> AUTUMN_ITEMS = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "autumn_crops"));
    private static final TagKey<Item> WINTER_ITEMS = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "winter_crops"));

    @Override
    public List<Season> getGrowingSeasons(Object entry) {
        List<Season> seasons = new ArrayList<>();

        if (entry instanceof Block block) {
            checkBlock(block, seasons);
            if (seasons.isEmpty()) {
                checkItem(block.asItem(), seasons);
            }
        } else if (entry instanceof Item item) {
            checkItem(item, seasons);
            if (seasons.isEmpty()) {
                Block block = Block.byItem(item);
                if (block != Blocks.AIR) {
                    checkBlock(block, seasons);
                }
            }
        }

        return seasons;
    }

    private void checkBlock(Block block, List<Season> seasons) {
        var state = block.defaultBlockState();
        if (state.is(SPRING_BLOCKS)) seasons.add(Season.SPRING);
        if (state.is(SUMMER_BLOCKS)) seasons.add(Season.SUMMER);
        if (state.is(AUTUMN_BLOCKS)) seasons.add(Season.AUTUMN);
        if (state.is(WINTER_BLOCKS)) seasons.add(Season.WINTER);
    }

    private void checkItem(Item item, List<Season> seasons) {
        if (item == Items.AIR) return;
        var stack = item.getDefaultInstance();
        if (stack.is(SPRING_ITEMS)) seasons.add(Season.SPRING);
        if (stack.is(SUMMER_ITEMS)) seasons.add(Season.SUMMER);
        if (stack.is(AUTUMN_ITEMS)) seasons.add(Season.AUTUMN);
        if (stack.is(WINTER_ITEMS)) seasons.add(Season.WINTER);
    }
}

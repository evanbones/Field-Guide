package com.evandev.fieldguide.compat.eclipticseasons;

import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.seasons.SeasonsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class EclipticSeasonsProvider implements SeasonsProvider {
    private static final String MOD_ID = "eclipticseasons";

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
        //? if <26.1 {
        block.defaultBlockState().getTags().forEach(tagKey -> processTag(tagKey, seasons));
        //?} else {
        /*block.defaultBlockState().tags().forEach(tagKey -> processTag(tagKey, seasons));
        *///?}
    }

    private void checkItem(Item item, List<Season> seasons) {
        if (item == Items.AIR) return;
        //? if <26.1 {
        item.getDefaultInstance().getTags().forEach(tagKey -> processTag(tagKey, seasons));
        //?} else {
        /*item.getDefaultInstance().tags().forEach(tagKey -> processTag(tagKey, seasons));
        *///?}
    }

    private void processTag(TagKey<?> tagKey, List<Season> seasons) {
        if (tagKey.location().getNamespace().equals(MOD_ID) && tagKey.location().getPath().startsWith("crops/")) {
            String path = tagKey.location().getPath();
            if (path.contains("spring") && !seasons.contains(Season.SPRING)) seasons.add(Season.SPRING);
            if (path.contains("summer") && !seasons.contains(Season.SUMMER)) seasons.add(Season.SUMMER);
            if (path.contains("autumn") && !seasons.contains(Season.AUTUMN)) seasons.add(Season.AUTUMN);
            if (path.contains("winter") && !seasons.contains(Season.WINTER)) seasons.add(Season.WINTER);
        }
    }
}

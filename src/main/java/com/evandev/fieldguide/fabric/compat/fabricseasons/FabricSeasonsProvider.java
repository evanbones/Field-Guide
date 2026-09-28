package com.evandev.fieldguide.fabric.compat.fabricseasons;

//? if <26.1 {
//? if fabric {
/*import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.seasons.SeasonsProvider;
import io.github.lucaargolo.seasons.resources.CropConfigs;
import io.github.lucaargolo.seasons.utils.SeasonalFertilizable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class FabricSeasonsProvider implements SeasonsProvider {

    private static final float GROWTH_THRESHOLD = 0.5f;

    @Override
    public List<Season> getGrowingSeasons(Object entry) {
        List<Season> seasons = new ArrayList<>();
        ResourceLocation registryId;
        Block targetBlock = null;

        if (entry instanceof Block block) {
            targetBlock = block;
        } else if (entry instanceof Item item) {
            if (item != Items.AIR) {
                Block block = Block.byItem(item);
                if (block != Blocks.AIR) {
                    targetBlock = block;
                }
            }
        }

        if (targetBlock instanceof SeasonalFertilizable) {
            registryId = BuiltInRegistries.BLOCK.getKey(targetBlock);

            if (!registryId.getPath().equals("air")) {
                checkSeasons(registryId, seasons);
            }
        }

        return seasons;
    }

    private void checkSeasons(ResourceLocation id, List<Season> seasons) {
        float springGrowth = CropConfigs.getSeasonCropMultiplier(id, io.github.lucaargolo.seasons.utils.Season.SPRING);
        float summerGrowth = CropConfigs.getSeasonCropMultiplier(id, io.github.lucaargolo.seasons.utils.Season.SUMMER);
        float fallGrowth = CropConfigs.getSeasonCropMultiplier(id, io.github.lucaargolo.seasons.utils.Season.FALL);
        float winterGrowth = CropConfigs.getSeasonCropMultiplier(id, io.github.lucaargolo.seasons.utils.Season.WINTER);

        if (springGrowth >= GROWTH_THRESHOLD) seasons.add(Season.SPRING);
        if (summerGrowth >= GROWTH_THRESHOLD) seasons.add(Season.SUMMER);
        if (fallGrowth >= GROWTH_THRESHOLD) seasons.add(Season.AUTUMN);
        if (winterGrowth >= GROWTH_THRESHOLD) seasons.add(Season.WINTER);
    }
}
*///?}
//?}

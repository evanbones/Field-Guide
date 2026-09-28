package com.evandev.fieldguide.api.seasons;

import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SeasonsAPI {
    private static final List<SeasonsProvider> PROVIDERS = new ArrayList<>();

    public static void registerProvider(SeasonsProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<Season> getGrowingSeasons(Object entry) {
        if (!ClientConfig.get().showSeasonIcons) {
            return List.of();
        }

        Set<Season> allSeasons = new LinkedHashSet<>();
        
        // Check the entry itself
        resolveAndCheck(entry, allSeasons);

        // If it's a GuideEntry, check children too
        if (entry instanceof GuideEntry ge) {
            if (ge.childEntries() != null) {
                for (ResourceLocation childId : ge.childEntries()) {
                    resolveAndCheck(childId, allSeasons);
                }
            }
        }

        return new ArrayList<>(allSeasons);
    }

    private static void resolveAndCheck(Object entry, Set<Season> allSeasons) {
        Object resolved = entry;
        if (entry instanceof ResourceLocation id) {
            resolved = resolveId(id);
        } else if (entry instanceof GuideEntry) {
            resolved = EntryResolver.resolveCoreEntry(entry);
        }

        if (resolved != null) {
            for (SeasonsProvider provider : PROVIDERS) {
                List<Season> seasons = provider.getGrowingSeasons(resolved);
                if (seasons != null) {
                    allSeasons.addAll(seasons);
                }
            }
        }
    }

    private static Object resolveId(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(Object.class::cast)
                .or(() -> BuiltInRegistries.ITEM.getOptional(id))
                .or(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(id))
                .orElse(null);
    }
}

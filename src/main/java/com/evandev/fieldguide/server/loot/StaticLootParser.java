package com.evandev.fieldguide.server.loot;

import com.evandev.fieldguide.compat.reliableremover.ReliableRemoverCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.mixin.accessor.*;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.data.ItemStackKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;

import java.util.*;

//? if <26.3 {
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
//?}

//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;
*///?}

public class StaticLootParser {
    private static final Map<LootTable, List<ParsedDrop>> TABLE_CACHE = Collections.synchronizedMap(new IdentityHashMap<>());

    public static void clearCache() {
        TABLE_CACHE.clear();
    }

    private static List<ParsedDrop> mergeDrops(List<ParsedDrop> drops) {
        Map<ItemStackKey, ParsedDrop> mergedMap = new LinkedHashMap<>();
        for (ParsedDrop drop : drops) {
            ItemStackKey key = new ItemStackKey(drop.stack);
            ParsedDrop existing = mergedMap.get(key);
            if (existing != null) {
                existing.chance += drop.chance;
                existing.minCount = Math.min(existing.minCount, drop.minCount);
                existing.maxCount = Math.max(existing.maxCount, drop.maxCount);
            } else {
                mergedMap.put(key, new ParsedDrop(drop.stack.copy(), drop.chance, drop.minCount, drop.maxCount));
            }
        }

        List<ParsedDrop> merged = new ArrayList<>(mergedMap.values());

        for (ParsedDrop drop : merged) {
            if (drop.chance > 1.0f) {
                drop.minCount = Math.round(drop.minCount * drop.chance);
                drop.maxCount = Math.round(drop.maxCount * drop.chance);
                drop.chance = 1.0f;
            }
        }

        merged.sort(Comparator.comparing(a -> a.stack.getHoverName().getString()));
        return merged;
    }

    public static List<ParsedDrop> parseTable(LootTable table, ServerLevel level) {
        if (TABLE_CACHE.containsKey(table)) {
            return TABLE_CACHE.get(table);
        }

        List<ParsedDrop> allDrops = new ArrayList<>();
        List<LootPool> pools = ((LootTableAccessor) table).fieldguide$getPools();

        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());

        for (LootPool pool : pools) {
            LootPoolAccessor poolAcc = (LootPoolAccessor) pool;
            //? if <26.3 {
            float poolRolls = getExpectedRolls(poolAcc.fieldguide$getRolls());
            float poolChance = getConditionChance(poolAcc.fieldguide$getConditions());
            //?} else {
            /*float poolRolls = getExpectedRolls(poolAcc.fieldguide$getRolls().value());
            float poolChance = getConditionChance(poolAcc.fieldguide$getCondition());
            *///?}

            List<LootPoolEntryContainer> entries = poolAcc.fieldguide$getEntries();
            int totalWeight = entries.stream().mapToInt(StaticLootParser::getEntryWeight).sum();

            for (LootPoolEntryContainer entry : entries) {
                parseEntry(entry, allDrops, poolRolls * poolChance, totalWeight, context);
            }
        }
        List<ParsedDrop> merged = mergeDrops(allDrops);
        TABLE_CACHE.put(table, merged);
        return merged;
    }

    private static void parseEntry(LootPoolEntryContainer entry, List<ParsedDrop> drops, float parentChance, int totalWeight, LootContext context) {
        //? if <26.3 {
        List<LootItemCondition> conditions = ((LootPoolEntryContainerAccessor) entry).fieldguide$getConditions();
        float entryConditionChance = getConditionChance(conditions);
        //?} else {
        /*LootPoolEntryContainerAccessor entryAcc = (LootPoolEntryContainerAccessor) entry;
        float entryConditionChance = getConditionChance(entryAcc.fieldguide$getCondition());
        *///?}

        int weight = getEntryWeight(entry);
        float selectionChance = totalWeight > 0 ? ((float) weight / totalWeight) : 1f;
        float branchChance = parentChance * selectionChance * entryConditionChance;

        if (branchChance <= 0) return;

        switch (entry) {
            case LootItem lootItem -> {
                Item item = ((LootItemAccessor) lootItem).fieldguide$getItem().value();
                ItemStack stack = new ItemStack(item);

                //? if <26.3 {
                List<LootItemFunction> functions = ((LootPoolSingletonContainerAccessor) lootItem).fieldguide$getFunctions();
                //?} else {
                /*List<LootItemFunction> functions = new ArrayList<>();
                entryAcc.fieldguide$getModifier().ifPresent(modifier -> flattenFunctions(modifier.value(), functions));
                *///?}

                int min = 1, max = 1;
                for (LootItemFunction function : functions) {
                    if (function instanceof SetItemCountFunction countFunc) {
                        //? if <26.1 {
                        NumberProvider provider = ((SetItemCountFunctionAccessor) countFunc).fieldguide$getValue();
                        min = Math.max(0, Math.round(getMinRolls(provider)));
                        max = Math.max(min, Math.round(getMaxRolls(provider)));
                        //?} else if <26.3 {
                        /*NumberProvider provider = ((SetItemCountFunctionAccessor) countFunc).fieldguide$getCount();
                        min = Math.max(0, Math.round(getMinRolls(provider)));
                        max = Math.max(min, Math.round(getMaxRolls(provider)));
                        *///?} else {
                        /*ContextIntProvider provider = ((SetItemCountFunctionAccessor) countFunc).fieldguide$getCount().value();
                        min = Math.max(0, getMinCount(provider));
                        max = Math.max(min, getMaxCount(provider));
                        *///?}

                        continue;
                    }
                    if (function.getClass().getName().startsWith("net.minecraft.")) {
                        try {
                            stack = function.apply(stack, context);
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (!stack.is(Items.AIR)) {
                    boolean isHidden = Services.PLATFORM.isModLoaded("reliable_remover") && ServerConfig.get().enableReliableRemover && ReliableRemoverCompat.isHidden(stack);
                    if (!isHidden) {
                        drops.add(new ParsedDrop(stack, branchChance, min, max));
                    }
                }
            }
            case NestedLootTable reference -> handleReference(reference, drops, branchChance, context);
            case CompositeEntryBase composite -> {
                List<LootPoolEntryContainer> children = ((CompositeEntryBaseAccessor) composite).fieldguide$getChildren();

                for (LootPoolEntryContainer child : children) {
                    parseEntry(child, drops, branchChance, 1, context);
                }
            }
            default -> {
            }
        }
    }

    //? if >=26.3 {
    /*private static void flattenFunctions(LootItemFunction function, List<LootItemFunction> out) {
        if (function instanceof SequenceFunction sequence) {
            for (Holder<LootItemFunction> child : ((SequenceFunctionAccessor) sequence).fieldguide$getFunctions()) {
                flattenFunctions(child.value(), out);
            }
        } else {
            out.add(function);
        }
    }
    *///?}

    private static void handleReference(NestedLootTable reference, List<ParsedDrop> drops,
                                        float branchChance, LootContext context) {
        //? if <26.3 {
        ((NestedLootTableAccessor) reference).fieldguide$getContents().left().ifPresent(tableKey -> {
            LootTable nestedTable = context.getLevel().getServer().reloadableRegistries().getLootTable(tableKey);
        //?} else {
        /*for (Holder<LootTable> holder : ((NestedLootTableAccessor) reference).fieldguide$getValue()) {
            LootTable nestedTable = holder.value();
        *///?}
            if (nestedTable != LootTable.EMPTY) {
                List<ParsedDrop> nestedDrops = parseTable(nestedTable, context.getLevel());
                for (ParsedDrop nested : nestedDrops) {
                    drops.add(new ParsedDrop(nested.stack, nested.chance * branchChance, nested.minCount, nested.maxCount));
                }
            }
        //? if <26.3 {
        });
        //?} else {
        /*}
        *///?}
    }

    private static int getEntryWeight(LootPoolEntryContainer entry) {
        //? if <26.3 {
        if (entry instanceof LootPoolSingletonContainer singleton) {
            return ((LootPoolSingletonContainerAccessor) singleton).fieldguide$getWeight();
        //?} else {
        /*if (entry instanceof UniformContainerBase uniform) {
            return ((LootPoolSingletonContainerAccessor) uniform).fieldguide$getWeight();
        *///?}
        }
        return 1;
    }

    //? if <26.3 {
    private static float getExpectedRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue(float value)) {
    //?} else {
    /*private static float getExpectedRolls(ContextIntProvider provider) {
        if (provider instanceof ConstantValue(int value)) {
    *///?}
            return value;
        //? if <26.3 {
        } else if (provider instanceof UniformGenerator(NumberProvider min1, NumberProvider max1)) {
            float min = getExpectedRolls(min1);
            float max = getExpectedRolls(max1);
            return (min + max) / 2.0f;
        //?} else {
        /*} else if (provider instanceof UniformGenerator(Holder<ContextIntProvider> min, Holder<ContextIntProvider> max)) {
            return (getExpectedRolls(min.value()) + getExpectedRolls(max.value())) / 2.0f;
        *///?}
        }
        return 1f;
    }

    //? if <26.3 {
    private static float getMinRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue(float value))
    //?} else {
    /*private static int getMinCount(ContextIntProvider provider) {
        if (provider instanceof ConstantValue(int value))
    *///?}
            return value;
        if (provider instanceof UniformGenerator uniform)
    //? if <26.3 {
            return getMinRolls(uniform.min());
    //?} else {
            /*return getMinCount(uniform.min().value());
        return 1;
    }

    private static int getMaxCount(ContextIntProvider provider) {
        if (provider instanceof ConstantValue(int value))
            return value;
        if (provider instanceof UniformGenerator uniform)
            return getMaxCount(uniform.max().value());
        return 1;
    }

    private static float getExpectedChance(ContextFloatProvider provider) {
        if (provider instanceof net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue(float value)) {
            return value;
        } else if (provider instanceof net.minecraft.world.level.storage.loot.providers.number.floats.UniformGenerator(Holder<ContextFloatProvider> min, Holder<ContextFloatProvider> max)) {
            return (getExpectedChance(min.value()) + getExpectedChance(max.value())) / 2.0f;
        }
    *///?}
        return 1f;
    }

    //? if <26.3 {
    private static float getMaxRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue(float value))
            return value;
        if (provider instanceof UniformGenerator uniform)
            return getMaxRolls(uniform.max());
        return 1f;
    //?} else {
    /*private static float getConditionChance(Optional<Holder<LootItemCondition>> condition) {
        return condition.map(holder -> getConditionChance(holder.value())).orElse(1.0f);
    *///?}
    }

    //? if <26.3 {
    private static float getConditionChance(List<LootItemCondition> conditions) {
        float chance = 1.0f;
        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemRandomChanceCondition) {
                NumberProvider chanceProvider = ((LootItemRandomChanceCondition) condition).chance();
                chance *= getExpectedRolls(chanceProvider);
            } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition lc) {
                chance *= lc.unenchantedChance();
            } else if (condition instanceof BonusLevelTableCondition tc) {
                if (!tc.values().isEmpty()) chance *= tc.values().getFirst();
    //?} else {
    /*private static float getConditionChance(LootItemCondition condition) {
        if (condition instanceof AllOfCondition allOf) {
            float chance = 1.0f;
            for (Holder<LootItemCondition> term : ((CompositeLootItemConditionAccessor) allOf).fieldguide$getTerms()) {
                chance *= getConditionChance(term.value());
    *///?}
            }
        //? if >=26.3 {
            /*return chance;
        } else if (condition instanceof LootItemRandomChanceCondition randomChance) {
            return getExpectedChance(randomChance.chance().value());
        } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition lc) {
            return lc.unenchantedChance();
        } else if (condition instanceof BonusLevelTableCondition tc) {
            if (!tc.values().isEmpty()) return tc.values().getFirst();
        *///?}
        }
        //? if <26.3 {
        return chance;
        //?} else {
        /*return 1.0f;
        *///?}
    }
}

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

import java.util.*;

//? if >=1.21 {
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
*///?}

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

    private static <T> List<T> list(List<T> values) {
        return values;
    }

    private static <T> List<T> list(T[] values) {
        return Arrays.asList(values);
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
        List<LootPool> pools = list(((LootTableAccessor) table).fieldguide$getPools());

        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        //? if >=1.21 {
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        //?} else {
        /*LootContext context = new LootContext.Builder(params).create(null);
        *///?}

        for (LootPool pool : pools) {
            LootPoolAccessor poolAcc = (LootPoolAccessor) pool;
            //? if <26.3 {
            float poolRolls = getExpectedRolls(poolAcc.fieldguide$getRolls());
            float poolChance = getConditionChance(list(poolAcc.fieldguide$getConditions()));
            //?} else {
            /*float poolRolls = getExpectedRolls(poolAcc.fieldguide$getRolls().value());
            float poolChance = getConditionChance(poolAcc.fieldguide$getCondition());
            *///?}

            List<LootPoolEntryContainer> entries = list(poolAcc.fieldguide$getEntries());
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
        List<LootItemCondition> conditions = list(((LootPoolEntryContainerAccessor) entry).fieldguide$getConditions());
        float entryConditionChance = getConditionChance(conditions);
        //?} else {
        /*LootPoolEntryContainerAccessor entryAcc = (LootPoolEntryContainerAccessor) entry;
        float entryConditionChance = getConditionChance(entryAcc.fieldguide$getCondition());
        *///?}

        int weight = getEntryWeight(entry);
        float selectionChance = totalWeight > 0 ? ((float) weight / totalWeight) : 1f;
        float branchChance = parentChance * selectionChance * entryConditionChance;

        if (branchChance <= 0) return;

        if (entry instanceof LootItem lootItem) {
            //? if >=1.21 {
            Item item = ((LootItemAccessor) lootItem).fieldguide$getItem().value();
            //?} else {
            /*Item item = ((LootItemAccessor) lootItem).fieldguide$getItem();
            *///?}
            ItemStack stack = new ItemStack(item);

            //? if <26.3 {
            List<LootItemFunction> functions = list(((LootPoolSingletonContainerAccessor) lootItem).fieldguide$getFunctions());
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
                boolean isHidden = Services.PLATFORM.isModLoaded("reliable_remover") && ServerConfig.getLocal().enableReliableRemover && ReliableRemoverCompat.isHidden(stack);
                if (!isHidden) {
                    drops.add(new ParsedDrop(stack, branchChance, min, max));
                }
            }
        } else if (entry instanceof CompositeEntryBase composite) {
            for (LootPoolEntryContainer child : list(((CompositeEntryBaseAccessor) composite).fieldguide$getChildren())) {
                parseEntry(child, drops, branchChance, 1, context);
            }
        //? if >=1.21 {
        } else if (entry instanceof NestedLootTable reference) {
        //?} else {
        /*} else if (entry instanceof LootTableReference reference) {
        *///?}
            handleReference(reference, drops, branchChance, context);
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

    //? if >=1.21 {
    private static void handleReference(NestedLootTable reference, List<ParsedDrop> drops,
                                        float branchChance, LootContext context) {
    //?} else {
    /*private static void handleReference(LootTableReference reference, List<ParsedDrop> drops,
                                        float branchChance, LootContext context) {
    *///?}
        for (LootTable nestedTable : resolveReference(reference, context)) {
            if (nestedTable != LootTable.EMPTY) {
                List<ParsedDrop> nestedDrops = parseTable(nestedTable, context.getLevel());
                for (ParsedDrop nested : nestedDrops) {
                    drops.add(new ParsedDrop(nested.stack, nested.chance * branchChance, nested.minCount, nested.maxCount));
                }
            }
        }
    }

    //? if >=1.21 {
    private static List<LootTable> resolveReference(NestedLootTable reference, LootContext context) {
    //?} else {
    /*private static List<LootTable> resolveReference(LootTableReference reference, LootContext context) {
    *///?}
        List<LootTable> tables = new ArrayList<>();
        //? if >=1.21 && <26.3 {
        ((NestedLootTableAccessor) reference).fieldguide$getContents().left()
                .ifPresent(tableKey -> tables.add(context.getLevel().getServer().reloadableRegistries().getLootTable(tableKey)));
        //?} else if >=26.3 {
        /*for (Holder<LootTable> holder : ((NestedLootTableAccessor) reference).fieldguide$getValue()) {
            tables.add(holder.value());
        }
        *///?} else {
        /*ResourceLocation tableId = ((LootTableReferenceAccessor) reference).fieldguide$getName();
        tables.add(context.getResolver().getLootTable(tableId));
        *///?}
        return tables;
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
    private static float constantValue(ConstantValue constant) {
        //? if >=1.21 {
        return constant.value();
        //?} else {
        /*return ((ConstantValueAccessor) (Object) constant).fieldguide$getValue();
        *///?}
    }

    private static NumberProvider uniformMin(UniformGenerator uniform) {
        //? if >=1.21 {
        return uniform.min();
        //?} else {
        /*return ((UniformGeneratorAccessor) uniform).fieldguide$getMin();
        *///?}
    }

    private static NumberProvider uniformMax(UniformGenerator uniform) {
        //? if >=1.21 {
        return uniform.max();
        //?} else {
        /*return ((UniformGeneratorAccessor) uniform).fieldguide$getMax();
        *///?}
    }

    private static float getExpectedRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue constant) {
            return constantValue(constant);
        } else if (provider instanceof UniformGenerator uniform) {
            float min = getExpectedRolls(uniformMin(uniform));
            float max = getExpectedRolls(uniformMax(uniform));
            return (min + max) / 2.0f;
        }
        return 1f;
    }

    private static float getMinRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue constant)
            return constantValue(constant);
        if (provider instanceof UniformGenerator uniform)
            return getMinRolls(uniformMin(uniform));
        return 1f;
    }

    private static float getMaxRolls(NumberProvider provider) {
        if (provider instanceof ConstantValue constant)
            return constantValue(constant);
        if (provider instanceof UniformGenerator uniform)
            return getMaxRolls(uniformMax(uniform));
        return 1f;
    }

    private static float getConditionChance(List<LootItemCondition> conditions) {
        float chance = 1.0f;
        for (LootItemCondition condition : conditions) {
            //? if >=1.21 {
            if (condition instanceof LootItemRandomChanceCondition randomChance) {
                chance *= getExpectedRolls(randomChance.chance());
            } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition lc) {
                chance *= lc.unenchantedChance();
            } else if (condition instanceof BonusLevelTableCondition tc) {
                if (!tc.values().isEmpty()) chance *= tc.values().get(0);
            }
            //?} else {
            /*if (condition instanceof LootItemRandomChanceCondition rc) {
                chance *= ((RandomChanceConditionAccessor) rc).fieldguide$getProbability();
            } else if (condition instanceof LootItemRandomChanceWithLootingCondition lc) {
                chance *= ((RandomChanceWithLootingConditionAccessor) lc).fieldguide$getPercent();
            } else if (condition instanceof BonusLevelTableCondition tc) {
                float[] values = ((BonusLevelTableConditionAccessor) tc).fieldguide$getValues();
                if (values.length > 0) chance *= values[0];
            }
            *///?}
        }
        return chance;
    }
    //?} else {
    /*private static float getExpectedRolls(ContextIntProvider provider) {
        if (provider instanceof ConstantValue(int value)) {
            return value;
        } else if (provider instanceof UniformGenerator(Holder<ContextIntProvider> min, Holder<ContextIntProvider> max)) {
            return (getExpectedRolls(min.value()) + getExpectedRolls(max.value())) / 2.0f;
        }
        return 1f;
    }

    private static int getMinCount(ContextIntProvider provider) {
        if (provider instanceof ConstantValue(int value))
            return value;
        if (provider instanceof UniformGenerator uniform)
            return getMinCount(uniform.min().value());
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
        return 1f;
    }

    private static float getConditionChance(Optional<Holder<LootItemCondition>> condition) {
        return condition.map(holder -> getConditionChance(holder.value())).orElse(1.0f);
    }

    private static float getConditionChance(LootItemCondition condition) {
        if (condition instanceof AllOfCondition allOf) {
            float chance = 1.0f;
            for (Holder<LootItemCondition> term : ((CompositeLootItemConditionAccessor) allOf).fieldguide$getTerms()) {
                chance *= getConditionChance(term.value());
            }
            return chance;
        } else if (condition instanceof LootItemRandomChanceCondition randomChance) {
            return getExpectedChance(randomChance.chance().value());
        } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition lc) {
            return lc.unenchantedChance();
        } else if (condition instanceof BonusLevelTableCondition tc) {
            if (!tc.values().isEmpty()) return tc.values().get(0);
        }
        return 1.0f;
    }
    *///?}
}

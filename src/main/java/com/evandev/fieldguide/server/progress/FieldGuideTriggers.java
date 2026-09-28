package com.evandev.fieldguide.server.progress;

import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

//? if <26.2 {
import net.minecraft.advancements.critereon.*;
//?}

//? if >=26.2 && <26.3 {
/*import net.minecraft.advancements.predicates.ContextAwarePredicate;
*///?}

//? if >=26.2 {
/*import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
*///?}

//? if >=26.3 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
*///?}

public class FieldGuideTriggers {
    public static Supplier<EntryUnlockedTrigger> ENTRY_UNLOCKED;
    public static Supplier<CategoryCompletedTrigger> CATEGORY_COMPLETED;
    public static Supplier<ScanEntityTrigger> SCAN_ENTITY;
    public static Supplier<ScanAndKillTrigger> SCAN_AND_KILL;

    public static void init() {
        ENTRY_UNLOCKED = Services.REGISTRY.registerCriterion("entry_unlocked", EntryUnlockedTrigger::new);
        CATEGORY_COMPLETED = Services.REGISTRY.registerCriterion("category_completed", CategoryCompletedTrigger::new);
        SCAN_ENTITY = Services.REGISTRY.registerCriterion("scan_entity", ScanEntityTrigger::new);
        SCAN_AND_KILL = Services.REGISTRY.registerCriterion("scan_and_kill", ScanAndKillTrigger::new);
    }

    public static class EntryUnlockedTrigger extends SimpleCriterionTrigger<EntryUnlockedTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, ResourceLocation entryId) {
            this.trigger(player, instance -> instance.matches(entryId));
        }

        //? if <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> entryId) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ResourceLocation> entryId) implements SimpleCriterionTrigger.SimpleInstance {
        *///?}
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    *///?}
                    ResourceLocation.CODEC.optionalFieldOf("entry").forGetter(TriggerInstance::entryId)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(ResourceLocation entryId) {
                if (this.entryId.isEmpty()) return true;
                ResourceLocation expected = this.entryId.get();
                if (expected.equals(entryId)) return true;
                return EntryResolver.rawIdsCompatible(expected, entryId);
            }
        }
    }

    public static class CategoryCompletedTrigger extends SimpleCriterionTrigger<CategoryCompletedTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, ResourceLocation categoryId) {
            this.trigger(player, instance -> instance.matches(categoryId));
        }

        //? if <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> categoryId) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ResourceLocation> categoryId) implements SimpleCriterionTrigger.SimpleInstance {
        *///?}
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    *///?}
                    ResourceLocation.CODEC.optionalFieldOf("category").forGetter(TriggerInstance::categoryId)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(ResourceLocation categoryId) {
                if (this.categoryId.isEmpty()) return true;
                ResourceLocation expected = this.categoryId.get();
                if (expected.equals(categoryId)) return true;

                if (!"minecraft".equals(expected.getNamespace()) || !categoryId.getPath().equals(expected.getPath())) {
                    return false;
                }
                return ServerFieldGuideManager.getInstance().getCategories().keySet().stream()
                        .filter(id -> id.getPath().equals(expected.getPath()))
                        .count() == 1;
            }
        }
    }

    public static class ScanEntityTrigger extends SimpleCriterionTrigger<ScanEntityTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, Entity entity) {
            LootContext lootContext = EntityPredicate.createContext(player, entity);
            this.trigger(player, instance -> instance.matches(lootContext));
        }

        //? if <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Holder<LootItemCondition>> entity) implements SimpleCriterionTrigger.SimpleInstance {
        *///?}
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    *///?}
            ).apply(instance, TriggerInstance::new));

            public boolean matches(LootContext lootContext) {
                //? if <26.3 {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
                //?} else {
                /*return this.entity.isEmpty() || this.entity.get().value().test(lootContext);
                *///?}
            }
        }
    }

    public static class ScanAndKillTrigger extends SimpleCriterionTrigger<ScanAndKillTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, Entity entity) {
            LootContext lootContext = EntityPredicate.createContext(player, entity);
            this.trigger(player, instance -> instance.matches(lootContext));
        }

        //? if <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Holder<LootItemCondition>> entity) implements SimpleCriterionTrigger.SimpleInstance {
        *///?}
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    *///?}
            ).apply(instance, TriggerInstance::new));

            public boolean matches(LootContext lootContext) {
                //? if <26.3 {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
                //?} else {
                /*return this.entity.isEmpty() || this.entity.get().value().test(lootContext);
                *///?}
            }
        }
    }
}

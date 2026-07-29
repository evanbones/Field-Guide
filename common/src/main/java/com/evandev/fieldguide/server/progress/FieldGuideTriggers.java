package com.evandev.fieldguide.server.progress;

import com.evandev.fieldguide.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

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

        public void trigger(ServerPlayer player, Identifier entryId) {
            this.trigger(player, instance -> instance.matches(entryId));
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Identifier> entryId) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    Identifier.CODEC.optionalFieldOf("entry").forGetter(TriggerInstance::entryId)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(Identifier entryId) {
                return this.entryId.isEmpty() || this.entryId.get().equals(entryId);
            }
        }
    }

    public static class CategoryCompletedTrigger extends SimpleCriterionTrigger<CategoryCompletedTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player, Identifier categoryId) {
            this.trigger(player, instance -> instance.matches(categoryId));
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Identifier> categoryId) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    Identifier.CODEC.optionalFieldOf("category").forGetter(TriggerInstance::categoryId)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(Identifier categoryId) {
                return this.categoryId.isEmpty() || this.categoryId.get().equals(categoryId);
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

        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(LootContext lootContext) {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
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

        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
            ).apply(instance, TriggerInstance::new));

            public boolean matches(LootContext lootContext) {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
            }
        }
    }
}

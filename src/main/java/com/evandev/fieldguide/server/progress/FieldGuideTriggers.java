package com.evandev.fieldguide.server.progress;

import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?} else {
/*import com.evandev.fieldguide.Constants;
import com.google.gson.JsonObject;
*///?}

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

    private static boolean matchesEntry(Optional<ResourceLocation> expectedId, ResourceLocation entryId) {
        if (expectedId.isEmpty()) return true;
        ResourceLocation expected = expectedId.get();
        if (expected.equals(entryId)) return true;
        return EntryResolver.rawIdsCompatible(expected, entryId);
    }

    private static boolean matchesCategory(Optional<ResourceLocation> expectedId, ResourceLocation categoryId) {
        if (expectedId.isEmpty()) return true;
        ResourceLocation expected = expectedId.get();
        if (expected.equals(categoryId)) return true;

        if (!"minecraft".equals(expected.getNamespace()) || !categoryId.getPath().equals(expected.getPath())) {
            return false;
        }
        return ServerFieldGuideManager.getInstance().getCategories().keySet().stream()
                .filter(id -> id.getPath().equals(expected.getPath()))
                .count() == 1;
    }

    //? if <1.21 {
    /*private static Optional<ResourceLocation> optionalId(JsonObject json, String key) {
        return json.has(key) ? Optional.of(ResourceLocation.parse(json.get(key).getAsString())) : Optional.empty();
    }

    private abstract static class LegacyTrigger<T extends AbstractCriterionTriggerInstance> extends SimpleCriterionTrigger<T> {
        private final ResourceLocation id;

        protected LegacyTrigger(String name) {
            this.id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name);
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return id;
        }
    }
    *///?}

    //? if >=1.21 {
    public static class EntryUnlockedTrigger extends SimpleCriterionTrigger<EntryUnlockedTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }
    //?} else {
    /*public static class EntryUnlockedTrigger extends LegacyTrigger<EntryUnlockedTrigger.TriggerInstance> {
        public EntryUnlockedTrigger() {
            super("entry_unlocked");
        }

        @Override
        protected @NotNull TriggerInstance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate player, @NotNull DeserializationContext context) {
            return new TriggerInstance(getId(), player, optionalId(json, "entry"));
        }
    *///?}

        public void trigger(ServerPlayer player, ResourceLocation entryId) {
            this.trigger(player, instance -> instance.matches(entryId));
        }

        //? if >=1.21 && <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> entryId) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else if >=26.3 {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ResourceLocation> entryId) implements SimpleCriterionTrigger.SimpleInstance {
        *///?} else {
        /*public static class TriggerInstance extends AbstractCriterionTriggerInstance {
            private final Optional<ResourceLocation> entryId;

            public TriggerInstance(ResourceLocation id, ContextAwarePredicate player, Optional<ResourceLocation> entryId) {
                super(id, player);
                this.entryId = entryId;
            }

            @Override
            public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                entryId.ifPresent(id -> json.addProperty("entry", id.toString()));
                return json;
            }
        *///?}
            //? if >=1.21 {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    *///?}
                    ResourceLocation.CODEC.optionalFieldOf("entry").forGetter(TriggerInstance::entryId)
            ).apply(instance, TriggerInstance::new));
            //?}

            public boolean matches(ResourceLocation entryId) {
                return matchesEntry(this.entryId, entryId);
            }
        }
    }

    //? if >=1.21 {
    public static class CategoryCompletedTrigger extends SimpleCriterionTrigger<CategoryCompletedTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }
    //?} else {
    /*public static class CategoryCompletedTrigger extends LegacyTrigger<CategoryCompletedTrigger.TriggerInstance> {
        public CategoryCompletedTrigger() {
            super("category_completed");
        }

        @Override
        protected @NotNull TriggerInstance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate player, @NotNull DeserializationContext context) {
            return new TriggerInstance(getId(), player, optionalId(json, "category"));
        }
    *///?}

        public void trigger(ServerPlayer player, ResourceLocation categoryId) {
            this.trigger(player, instance -> instance.matches(categoryId));
        }

        //? if >=1.21 && <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceLocation> categoryId) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else if >=26.3 {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ResourceLocation> categoryId) implements SimpleCriterionTrigger.SimpleInstance {
        *///?} else {
        /*public static class TriggerInstance extends AbstractCriterionTriggerInstance {
            private final Optional<ResourceLocation> categoryId;

            public TriggerInstance(ResourceLocation id, ContextAwarePredicate player, Optional<ResourceLocation> categoryId) {
                super(id, player);
                this.categoryId = categoryId;
            }

            @Override
            public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                categoryId.ifPresent(id -> json.addProperty("category", id.toString()));
                return json;
            }
        *///?}
            //? if >=1.21 {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    *///?}
                    ResourceLocation.CODEC.optionalFieldOf("category").forGetter(TriggerInstance::categoryId)
            ).apply(instance, TriggerInstance::new));
            //?}

            public boolean matches(ResourceLocation categoryId) {
                return matchesCategory(this.categoryId, categoryId);
            }
        }
    }

    //? if >=1.21 {
    public static class ScanEntityTrigger extends SimpleCriterionTrigger<ScanEntityTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }
    //?} else {
    /*public static class ScanEntityTrigger extends LegacyTrigger<ScanEntityTrigger.TriggerInstance> {
        public ScanEntityTrigger() {
            super("scan_entity");
        }

        @Override
        protected @NotNull TriggerInstance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate player, @NotNull DeserializationContext context) {
            return new TriggerInstance(getId(), player, EntityPredicate.fromJson(json, "entity", context));
        }
    *///?}

        public void trigger(ServerPlayer player, Entity entity) {
            LootContext lootContext = EntityPredicate.createContext(player, entity);
            this.trigger(player, instance -> instance.matches(lootContext));
        }

        //? if >=1.21 && <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else if >=26.3 {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Holder<LootItemCondition>> entity) implements SimpleCriterionTrigger.SimpleInstance {
        *///?} else {
        /*public static class TriggerInstance extends AbstractCriterionTriggerInstance {
            private final ContextAwarePredicate entity;

            public TriggerInstance(ResourceLocation id, ContextAwarePredicate player, ContextAwarePredicate entity) {
                super(id, player);
                this.entity = entity;
            }

            @Override
            public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                json.add("entity", entity.toJson(context));
                return json;
            }
        *///?}
            //? if >=1.21 {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    *///?}
            ).apply(instance, TriggerInstance::new));
            //?}

            public boolean matches(LootContext lootContext) {
                //? if >=1.21 && <26.3 {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
                //?} else if >=26.3 {
                /*return this.entity.isEmpty() || this.entity.get().value().test(lootContext);
                *///?} else {
                /*return this.entity.matches(lootContext);
                *///?}
            }
        }
    }

    //? if >=1.21 {
    public static class ScanAndKillTrigger extends SimpleCriterionTrigger<ScanAndKillTrigger.TriggerInstance> {
        @Override
        public @NotNull Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }
    //?} else {
    /*public static class ScanAndKillTrigger extends LegacyTrigger<ScanAndKillTrigger.TriggerInstance> {
        public ScanAndKillTrigger() {
            super("scan_and_kill");
        }

        @Override
        protected @NotNull TriggerInstance createInstance(@NotNull JsonObject json, @NotNull ContextAwarePredicate player, @NotNull DeserializationContext context) {
            return new TriggerInstance(getId(), player, EntityPredicate.fromJson(json, "entity", context));
        }
    *///?}

        public void trigger(ServerPlayer player, Entity entity) {
            LootContext lootContext = EntityPredicate.createContext(player, entity);
            this.trigger(player, instance -> instance.matches(lootContext));
        }

        //? if >=1.21 && <26.3 {
        public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity) implements SimpleCriterionTrigger.SimpleInstance {
        //?} else if >=26.3 {
        /*public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Holder<LootItemCondition>> entity) implements SimpleCriterionTrigger.SimpleInstance {
        *///?} else {
        /*public static class TriggerInstance extends AbstractCriterionTriggerInstance {
            private final ContextAwarePredicate entity;

            public TriggerInstance(ResourceLocation id, ContextAwarePredicate player, ContextAwarePredicate entity) {
                super(id, player);
                this.entity = entity;
            }

            @Override
            public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                json.add("entity", entity.toJson(context));
                return json;
            }
        *///?}
            //? if >=1.21 {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    //? if <26.3 {
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    //?} else {
                    /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity)
                    *///?}
            ).apply(instance, TriggerInstance::new));
            //?}

            public boolean matches(LootContext lootContext) {
                //? if >=1.21 && <26.3 {
                return this.entity.isEmpty() || this.entity.get().matches(lootContext);
                //?} else if >=26.3 {
                /*return this.entity.isEmpty() || this.entity.get().value().test(lootContext);
                *///?} else {
                /*return this.entity.matches(lootContext);
                *///?}
            }
        }
    }
}

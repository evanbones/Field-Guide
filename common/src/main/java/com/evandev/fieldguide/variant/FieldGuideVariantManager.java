package com.evandev.fieldguide.variant;

import com.evandev.fieldguide.api.variant.DatapackVariant;
import com.evandev.fieldguide.api.variant.DatapackVariantDefinition;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class FieldGuideVariantManager {

    private static final Map<Class<?>, VariantProvider<?>> PROVIDERS = new HashMap<>();
    private static final Map<String, List<VariantDef>> VARIANT_CACHE = new HashMap<>();
    private static final Map<String, List<VariantDef>> ENTITY_TYPE_VARIANT_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, DatapackVariantDefinition> DATAPACK_VARIANTS = new HashMap<>();

    private static final Set<Class<?>> FAILED_REFLECTION = new HashSet<>();

    static {
        // Sheep
        registerProvider(Sheep.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Sheep entity) {
                return Arrays.stream(DyeColor.values()).map(c -> new VariantDef(c.getName(), c)).toList();
            }

            @Override
            public void apply(Sheep entity, VariantDef def) {
                if (def.value() instanceof DyeColor color) entity.setColor(color);
            }

            @Override
            public VariantDef getCurrent(Sheep entity) {
                return new VariantDef(entity.getColor().getName(), entity.getColor());
            }
        });

        // Horse
        registerProvider(Horse.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Horse entity) {
                return Arrays.stream(Variant.values()).map(v -> new VariantDef(v.name(), v)).toList();
            }

            @Override
            public void apply(Horse entity, VariantDef def) {
                if (def.value() instanceof Variant variant) entity.setVariant(variant);
            }

            @Override
            public VariantDef getCurrent(Horse entity) {
                return new VariantDef(entity.getVariant().name(), entity.getVariant());
            }
        });

        // Llama
        registerProvider(Llama.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Llama entity) {
                return Arrays.stream(Llama.Variant.values()).map(v -> new VariantDef(v.name(), v)).toList();
            }

            @Override
            public void apply(Llama entity, VariantDef def) {
                if (def.value() instanceof Llama.Variant variant) entity.setVariant(variant);
            }

            @Override
            public VariantDef getCurrent(Llama entity) {
                return new VariantDef(entity.getVariant().name(), entity.getVariant());
            }
        });

        // Axolotl
        registerProvider(Axolotl.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Axolotl entity) {
                return Arrays.stream(Axolotl.Variant.values())
                        .map(v -> new VariantDef(v.getSerializedName(), v))
                        .toList();
            }

            @Override
            public void apply(Axolotl entity, VariantDef def) {
                if (def.value() instanceof Axolotl.Variant type) entity.setVariant(type);
            }

            @Override
            public VariantDef getCurrent(Axolotl entity) {
                return new VariantDef(entity.getVariant().getSerializedName(), entity.getVariant());
            }
        });

        // Mooshroom
        registerProvider(MushroomCow.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(MushroomCow entity) {
                return Arrays.stream(MushroomCow.MushroomType.values())
                        .map(v -> new VariantDef(v.getSerializedName(), v))
                        .toList();
            }

            @Override
            public void apply(MushroomCow entity, VariantDef def) {
                if (def.value() instanceof MushroomCow.MushroomType type) entity.setVariant(type);
            }

            @Override
            public VariantDef getCurrent(MushroomCow entity) {
                return new VariantDef(entity.getVariant().getSerializedName(), entity.getVariant());
            }
        });

        // Fox
        registerProvider(Fox.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Fox entity) {
                return Arrays.stream(Fox.Type.values()).map(v -> new VariantDef(v.getSerializedName(), v)).toList();
            }

            @Override
            public void apply(Fox entity, VariantDef def) {
                if (def.value() instanceof Fox.Type type) entity.setVariant(type);
            }

            @Override
            public VariantDef getCurrent(Fox entity) {
                return new VariantDef(entity.getVariant().getSerializedName(), entity.getVariant());
            }
        });

        // Parrot
        registerProvider(Parrot.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Parrot entity) {
                return Arrays.stream(Parrot.Variant.values()).map(v -> new VariantDef(v.getSerializedName(), v)).toList();
            }

            @Override
            public void apply(Parrot entity, VariantDef def) {
                if (def.value() instanceof Parrot.Variant type)
                    entity.setVariant(type);
            }

            @Override
            public VariantDef getCurrent(Parrot entity) {
                return new VariantDef(entity.getVariant().getSerializedName(), entity.getVariant());
            }
        });

        // Rabbit
        registerProvider(Rabbit.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Rabbit entity) {
                return Arrays.stream(Rabbit.Variant.values()).map(v -> new VariantDef(v.getSerializedName(), v)).toList();
            }

            @Override
            public void apply(Rabbit entity, VariantDef def) {
                if (def.value() instanceof Rabbit.Variant type)
                    entity.setVariant(type);
            }

            @Override
            public VariantDef getCurrent(Rabbit entity) {
                return new VariantDef(entity.getVariant().getSerializedName(), entity.getVariant());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> void registerProvider(Class<T> entityClass, VariantProvider<T> provider) {
        if (PROVIDERS.containsKey(entityClass)) {
            VariantProvider<T> existing = (VariantProvider<T>) PROVIDERS.get(entityClass);
            if (existing instanceof CompositeVariantProvider<T> composite) {
                if (composite.providers.stream().noneMatch(p -> p.getClass().equals(provider.getClass()))) {
                    composite.addProvider(provider);
                }
            } else if (!existing.getClass().equals(provider.getClass())) {
                CompositeVariantProvider<T> composite = new CompositeVariantProvider<>(existing);
                composite.addProvider(provider);
                PROVIDERS.put(entityClass, composite);
            }
        } else {
            PROVIDERS.put(entityClass, provider);
        }
    }

    public static void setDatapackVariants(Map<ResourceLocation, DatapackVariantDefinition> variants) {
        DATAPACK_VARIANTS.clear();

        variants.forEach((entityId, variantDef) -> {
            if (!entityId.getNamespace().equals("minecraft") && !Services.PLATFORM.isModLoaded(entityId.getNamespace())) {
                return;
            }

            List<DatapackVariant> validVariants = variantDef.variants().stream().filter(variant -> {
                if (variant.id().contains(":")) {
                    String modId = variant.id().substring(0, variant.id().indexOf(':'));
                    return modId.equals("minecraft") || Services.PLATFORM.isModLoaded(modId);
                }
                return true;
            }).toList();

            DATAPACK_VARIANTS.put(entityId, new DatapackVariantDefinition(variantDef.replace(), validVariants));
        });

        VARIANT_CACHE.clear();
        ENTITY_TYPE_VARIANT_CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> VariantProvider<T> getProvider(Entity entity) {
        if (ServerConfig.get().disableVariants) return null;
        if (!(entity instanceof Mob mob)) return null;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        VariantProvider<T> classProvider = getProvider((Class<T>) mob.getClass());

        if (!FAILED_REFLECTION.contains(mob.getClass())) {
            if (PROVIDERS.containsKey(mob.getClass())) {
                FAILED_REFLECTION.add(mob.getClass());
            } else {
                VariantProvider<T> refl = (VariantProvider<T>) getReflectionProvider(mob);
                if (refl != null) {
                    registerProvider((Class<T>) mob.getClass(), refl);
                    classProvider = getProvider((Class<T>) mob.getClass());
                } else {
                    FAILED_REFLECTION.add(mob.getClass());
                }
            }
        }

        if (DATAPACK_VARIANTS.containsKey(entityId)) {
            DatapackVariantDefinition def = DATAPACK_VARIANTS.get(entityId);
            VariantProvider<T> dataProvider = (VariantProvider<T>) getDatapackProvider(entityId);

            if (def.replace()) {
                return dataProvider;
            }

            if (classProvider != null) {
                if (classProvider instanceof CompositeVariantProvider<T> comp) {
                    comp.addProvider(dataProvider);
                    return comp;
                }
                CompositeVariantProvider<T> composite = new CompositeVariantProvider<>(classProvider);
                composite.addProvider(dataProvider);
                return composite;
            }
            return dataProvider;
        }

        return classProvider;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> VariantProvider<T> getProvider(Class<T> entityClass) {
        List<VariantProvider<T>> matching = new ArrayList<>();
        Class<?> clazz = entityClass;

        while (clazz != null && clazz != Object.class) {
            if (PROVIDERS.containsKey(clazz)) {
                VariantProvider<T> p = (VariantProvider<T>) PROVIDERS.get(clazz);
                if (p instanceof CompositeVariantProvider<T> comp) {
                    for (VariantProvider<T> inner : comp.providers) {
                        if (matching.stream().noneMatch(m -> m.getClass().equals(inner.getClass()))) {
                            matching.add(inner);
                        }
                    }
                } else {
                    if (matching.stream().noneMatch(m -> m.getClass().equals(p.getClass()))) {
                        matching.add(p);
                    }
                }
            }
            if (clazz == Mob.class) break;
            clazz = clazz.getSuperclass();
        }

        if (VillagerDataHolder.class.isAssignableFrom(entityClass)) {
            VariantProvider<T> vp = (VariantProvider<T>) getVillagerProvider();
            if (matching.stream().noneMatch(m -> m.getClass().equals(vp.getClass()))) {
                matching.add(vp);
            }
        }

        if (matching.isEmpty()) {
            return null;
        }

        if (matching.size() == 1) return matching.getFirst();

        CompositeVariantProvider<T> composite = new CompositeVariantProvider<>(matching.getFirst());
        for (int i = 1; i < matching.size(); i++) {
            composite.addProvider(matching.get(i));
        }
        return composite;
    }

    public static List<VariantDef> getVariants(Entity entity) {
        if (ServerConfig.get().disableVariants) return List.of();
        if (!(entity instanceof Mob mob)) return List.of();

        VariantProvider<Mob> provider = getProvider(entity);

        if (provider != null) {
            String cacheKey = provider.getCacheKey(mob);
            if (VARIANT_CACHE.containsKey(cacheKey)) {
                return VARIANT_CACHE.get(cacheKey);
            }

            List<VariantDef> variants = provider.getVariants(mob);

            if (variants.size() <= 1) {
                variants = List.of();
            }

            VARIANT_CACHE.put(cacheKey, variants);
            return variants;
        }

        return List.of();
    }

    public static List<VariantDef> getVariants(EntityType<?> type, Level level) {
        if (ServerConfig.get().disableVariants) return List.of();

        if (level != null) {
            try {
                Entity entity = type.create(level);
                if (entity instanceof Mob mob) {
                    VariantProvider<Mob> provider = getProvider(entity);
                    String cacheKey = provider != null ? provider.getCacheKey(mob) : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();

                    if (ENTITY_TYPE_VARIANT_CACHE.containsKey(cacheKey)) {
                        return ENTITY_TYPE_VARIANT_CACHE.get(cacheKey);
                    }

                    List<VariantDef> variants = getVariants(mob);
                    ENTITY_TYPE_VARIANT_CACHE.put(cacheKey, variants);
                    return variants;
                }
            } catch (Exception ignored) {
            }
        }

        return List.of();
    }

    public static List<String> getVariantIds(EntityType<?> type, Level level) {
        return getVariants(type, level).stream().map(VariantDef::id).toList();
    }

    public static Component getVariantDisplayName(VariantDef variant) {
        String name = variant.id();
        if (name.contains(":")) name = name.substring(name.indexOf(':') + 1);
        if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);

        name = Arrays.stream(name.split("_"))
                .map(s -> s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        return Component.literal(name);
    }

    private static VariantProvider<Mob> getDatapackProvider(ResourceLocation entityId) {
        DatapackVariantDefinition def = DATAPACK_VARIANTS.get(entityId);
        if (def == null) return null;
        List<DatapackVariant> variants = def.variants();

        return new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Mob entity) {
                return variants.stream().map(v -> new VariantDef(v.id(), v.nbt())).toList();
            }

            @Override
            public void apply(Mob entity, VariantDef def) {
                if (def.value() instanceof CompoundTag nbt) {
                    if ("default".equals(def.id())) {
                        entity.load(nbt);
                    } else {
                        CompoundTag current = new CompoundTag();
                        entity.saveWithoutId(current);
                        current.merge(nbt);
                        entity.load(current);
                    }
                }
            }

            @Override
            public VariantDef getCurrent(Mob entity) {
                CompoundTag entityNbt = new CompoundTag();
                entity.saveWithoutId(entityNbt);
                for (DatapackVariant v : variants) {
                    if (NbtUtils.compareNbt(v.nbt(), entityNbt, true)) {
                        return new VariantDef(v.id(), v.nbt());
                    }
                }
                return new VariantDef("default", entityNbt);
            }

            @Override
            public String getCacheKey(Mob entity) {
                return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "_datapack";
            }
        };
    }

    private static VariantProvider<Mob> getVillagerProvider() {
        return new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Mob entity) {
                return BuiltInRegistries.VILLAGER_TYPE.entrySet().stream()
                        .map(e -> new VariantDef(e.getKey().location().toString(), e.getValue()))
                        .toList();
            }

            @Override
            public void apply(Mob entity, VariantDef def) {
                if (entity instanceof VillagerDataHolder holder)
                    holder.setVillagerData(holder.getVillagerData().setType((VillagerType) def.value()));
            }

            @Override
            public VariantDef getCurrent(Mob entity) {
                if (entity instanceof VillagerDataHolder holder)
                    return new VariantDef(BuiltInRegistries.VILLAGER_TYPE.getKey(holder.getVillagerData().getType()).toString(), holder.getVillagerData().getType());
                return new VariantDef("default", null);
            }
        };
    }

    private static VariantProvider<Mob> getReflectionProvider(Mob mob) {
        Class<?> clazz = mob.getClass();
        while (clazz != null && clazz != Mob.class && clazz != Object.class) {
            Method[] methods;
            try {
                methods = clazz.getDeclaredMethods();
            } catch (NoClassDefFoundError | RuntimeException e) {
                clazz = clazz.getSuperclass();
                continue;
            }
            for (Method m : methods) {
                String name = m.getName();
                if (m.getParameterCount() == 0 && (name.startsWith("get") || name.startsWith("is")) &&
                        (name.contains("Variant") || name.contains("Variation") || name.contains("Type") || name.contains("Color")) &&
                        !name.equals("getCollarColor") && !name.contains("Order") && !name.contains("Mode") && !name.contains("Status") &&
                        !name.contains("Behaviour") && !name.contains("Accessibility") && !name.contains("State") &&
                        !name.contains("SpawnType")) {
                    if (!m.getReturnType().isEnum() || m.getReturnType().getSimpleName().equals("DyeColor")) continue;
                    String suffix = name.startsWith("get") ? name.substring(3) : name.substring(2);
                    try {
                        Method potentialSetter = mob.getClass().getMethod("set" + suffix, m.getReturnType());
                        final Method finalGetter = m;
                        final Method finalSetter = potentialSetter;
                        return new VariantProvider<>() {
                            @Override
                            public List<VariantDef> getVariants(Mob entity) {
                                return Arrays.stream(finalGetter.getReturnType().getEnumConstants()).map(e -> new VariantDef(((Enum<?>) e).name(), e)).toList();
                            }

                            @Override
                            public void apply(Mob entity, VariantDef def) {
                                try {
                                    finalSetter.invoke(entity, def.value());
                                } catch (Exception ignored) {
                                }
                            }

                            @Override
                            public VariantDef getCurrent(Mob entity) {
                                try {
                                    Object val = finalGetter.invoke(entity);
                                    if (val instanceof Enum<?> e) return new VariantDef(e.name(), e);
                                } catch (Exception ignored) {
                                }
                                return new VariantDef("default", null);
                            }
                        };
                    } catch (NoSuchMethodException | NoClassDefFoundError | RuntimeException ignored) {
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static class CompositeVariantProvider<T extends Mob> implements VariantProvider<T> {
        private final List<VariantProvider<T>> providers = new ArrayList<>();

        public CompositeVariantProvider(VariantProvider<T> first) {
            providers.add(first);
        }

        public void addProvider(VariantProvider<T> provider) {
            providers.add(provider);
        }

        @Override
        public List<VariantDef> getVariants(T entity) {
            List<VariantDef> list = providers.stream()
                    .flatMap(p -> p.getVariants(entity).stream())
                    .distinct()
                    .toList();

            boolean hasMLVariants = list.stream().anyMatch(v -> v.value() instanceof ResourceLocation);
            boolean hasVanillaEnums = list.stream().anyMatch(v -> v.value() instanceof Enum<?>);
            boolean hasVanilla = list.stream().anyMatch(v -> v.value() != null && !(v.value() instanceof ResourceLocation));
            boolean suppressDefault = providers.stream().anyMatch(p -> p.suppressesDefaultVariant(entity));

            List<VariantDef> result = new ArrayList<>();
            if (hasMLVariants && hasVanillaEnums && suppressDefault) {
                list.stream().filter(v -> !(v.value() instanceof Enum<?>)).forEach(result::add);
            } else if (hasVanilla) {
                for (VariantDef v : list) {
                    if (v.id().equals("default") && v.value() == null) continue;
                    result.add(v);
                }
            } else {
                boolean hasDefault = list.stream().anyMatch(v -> v.id().equals("default") && v.value() == null);
                if (!suppressDefault && !hasDefault && !list.isEmpty()) {
                    result.add(new VariantDef("default", null));
                }
                result.addAll(list);
            }

            return result.stream().distinct().toList();
        }

        @Override
        public boolean suppressesDefaultVariant(T entity) {
            return providers.stream().anyMatch(p -> p.suppressesDefaultVariant(entity));
        }

        @Override
        public void apply(T entity, VariantDef def) {
            if (def.value() instanceof List<?> list) {
                for (int i = 0; i < providers.size(); i++) {
                    if (i < list.size() && list.get(i) instanceof VariantDef subDef) {
                        providers.get(i).apply(entity, subDef);
                    }
                }
            } else {
                for (VariantProvider<T> p : providers) {
                    p.apply(entity, def);
                }
            }
        }

        @Override
        public VariantDef getCurrent(T entity) {
            List<VariantDef> states = new ArrayList<>();
            String mainId = "default";
            for (VariantProvider<T> p : providers) {
                VariantDef current = p.getCurrent(entity);
                if (current != null) {
                    states.add(current);
                    if (!current.id().equals("default")) {
                        mainId = current.id();
                    }
                } else {
                    states.add(new VariantDef("default", null));
                }
            }
            return new VariantDef(mainId, states);
        }

        @Override
        public String getCacheKey(T entity) {
            return providers.stream().map(p -> p.getCacheKey(entity)).collect(Collectors.joining("_"));
        }
    }
}
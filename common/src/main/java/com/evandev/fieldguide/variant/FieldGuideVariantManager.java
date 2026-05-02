package com.evandev.fieldguide.variant;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.variant.DatapackVariant;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class FieldGuideVariantManager {

    private static final Map<Class<?>, VariantProvider<?>> PROVIDERS = new HashMap<>();
    private static final Map<String, List<VariantDef>> VARIANT_CACHE = new HashMap<>();
    private static final Map<String, List<VariantDef>> ENTITY_TYPE_VARIANT_CACHE = new HashMap<>();
    private static final Map<Identifier, List<DatapackVariant>> DATAPACK_VARIANTS = new HashMap<>();
    private static final Set<Class<?>> FAILED_REFLECTION = new HashSet<>();

    static {
        // Sheep
        registerProvider(Sheep.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Sheep entity) {
                return Arrays.stream(DyeColor.values())
                        .map(c -> new VariantDef(c.getName(), c))
                        .toList();
            }

            @Override
            public void apply(Sheep entity, VariantDef def) {
                if (def.value() instanceof DyeColor color) {
                    entity.setColor(color);
                }
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
                return Arrays.stream(Variant.values())
                        .map(v -> new VariantDef(v.name(), v))
                        .toList();
            }

            @Override
            public void apply(Horse entity, VariantDef def) {
                if (def.value() instanceof Variant variant) {
                    try {
                        Method m = Horse.class.getDeclaredMethod("setVariant", Variant.class);
                        m.setAccessible(true);
                        m.invoke(entity, variant);
                    } catch (Exception e) {
                        // Fallback reflection handled
                    }
                }
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
                return Arrays.stream(Llama.Variant.values())
                        .map(v -> new VariantDef(v.name(), v))
                        .toList();
            }

            @Override
            public void apply(Llama entity, VariantDef def) {
                if (def.value() instanceof Llama.Variant variant) {
                    try {
                        Method m = Llama.class.getDeclaredMethod("setVariant", Llama.Variant.class);
                        m.setAccessible(true);
                        m.invoke(entity, variant);
                    } catch (Exception e) {
                        // Fallback reflection handled
                    }
                }
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
                        .map(v -> new VariantDef(v.getName(), v))
                        .toList();
            }

            @Override
            public void apply(Axolotl entity, VariantDef def) {
                if (def.value() instanceof Axolotl.Variant variant) {
                    try {
                        Method m = Axolotl.class.getDeclaredMethod("setVariant", Axolotl.Variant.class);
                        m.setAccessible(true);
                        m.invoke(entity, variant);
                    } catch (Exception e) {
                        // Fallback reflection handled
                    }
                }
            }

            @Override
            public VariantDef getCurrent(Axolotl entity) {
                return new VariantDef(entity.getVariant().getName(), entity.getVariant());
            }
        });

        // Tropical Fish
        registerProvider(TropicalFish.class, new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(TropicalFish entity) {
                return TropicalFish.COMMON_VARIANTS.stream()
                        .map(v -> {
                            String id = v.pattern().getSerializedName() + "_" +
                                    v.baseColor().getName() + "_" +
                                    v.patternColor().getName();
                            return new VariantDef(id, v);
                        })
                        .toList();
            }

            @Override
            public void apply(TropicalFish entity, VariantDef def) {
                if (def.value() instanceof TropicalFish.Variant variant) {
                    try {
                        Method m = TropicalFish.class.getDeclaredMethod("setPackedVariant", int.class);
                        m.setAccessible(true);
                        m.invoke(entity, variant.getPackedId());
                    } catch (Exception e) {
                        // Fallback reflection handled
                    }
                }
            }

            @Override
            public VariantDef getCurrent(TropicalFish entity) {
                TropicalFish.Variant currentVar = new TropicalFish.Variant(
                        entity.getPattern(),
                        entity.getBaseColor(),
                        entity.getPatternColor()
                );

                String id = currentVar.pattern().getSerializedName() + "_" +
                        currentVar.baseColor().getName() + "_" +
                        currentVar.patternColor().getName();

                return new VariantDef(id, currentVar);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> void registerProvider(Class<T> entityClass, VariantProvider<T> provider) {
        if (PROVIDERS.containsKey(entityClass)) {
            VariantProvider<T> existing = (VariantProvider<T>) PROVIDERS.get(entityClass);
            if (existing instanceof CompositeVariantProvider) {
                ((CompositeVariantProvider<T>) existing).addProvider(provider);
            } else {
                CompositeVariantProvider<T> composite = new CompositeVariantProvider<>(existing);
                composite.addProvider(provider);
                PROVIDERS.put(entityClass, composite);
            }
        } else {
            PROVIDERS.put(entityClass, provider);
        }
    }

    public static void setDatapackVariants(Map<Identifier, List<DatapackVariant>> variants) {
        DATAPACK_VARIANTS.clear();

        variants.forEach((entityId, datapackVariants) -> {
            if (!entityId.getNamespace().equals("minecraft") && !Services.PLATFORM.isModLoaded(entityId.getNamespace())) {
                return;
            }

            List<DatapackVariant> validVariants = datapackVariants.stream().filter(variant -> {
                if (variant.id().contains(":")) {
                    String modId = variant.id().substring(0, variant.id().indexOf(':'));
                    return modId.equals("minecraft") || Services.PLATFORM.isModLoaded(modId);
                }
                return true;
            }).toList();

            if (!validVariants.isEmpty()) {
                DATAPACK_VARIANTS.put(entityId, validVariants);
            }
        });

        VARIANT_CACHE.clear();
        ENTITY_TYPE_VARIANT_CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> VariantProvider<T> getProvider(Entity entity) {
        if (ServerConfig.get().disableVariants) return null;
        if (!(entity instanceof Mob mob)) return null;

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        if (DATAPACK_VARIANTS.containsKey(entityId)) {
            return (VariantProvider<T>) getDatapackProvider(entityId);
        }

        VariantProvider<T> classProvider = getProvider((Class<T>) mob.getClass());
        if (classProvider == null && !FAILED_REFLECTION.contains(mob.getClass())) {
            classProvider = (VariantProvider<T>) getReflectionProvider(mob);
            if (classProvider != null) {
                registerProvider((Class<T>) mob.getClass(), classProvider);
            } else {
                FAILED_REFLECTION.add(mob.getClass());
            }
        }

        return classProvider;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> VariantProvider<T> getProvider(Class<T> entityClass) {
        if (ServerConfig.get().disableVariants) return null;
        List<VariantProvider<T>> matching = new ArrayList<>();
        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Object.class) {
            if (PROVIDERS.containsKey(clazz)) {
                matching.add((VariantProvider<T>) PROVIDERS.get(clazz));
            }
            if (clazz == Mob.class) break;
            clazz = clazz.getSuperclass();
        }

        if (matching.isEmpty()) {
            if (VillagerDataHolder.class.isAssignableFrom(Objects.requireNonNull(entityClass))) {
                return (VariantProvider<T>) getVillagerProvider();
            }
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
            VARIANT_CACHE.put(cacheKey, variants);
            return variants;
        }

        return List.of();
    }

    public static List<VariantDef> getVariants(EntityType<?> type, Level level) {
        if (ServerConfig.get().disableVariants) return List.of();

        if (level != null) {
            try {
                Entity entity = type.create(level, EntitySpawnReason.COMMAND);
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

    private static VariantProvider<Mob> getDatapackProvider(Identifier entityId) {
        List<DatapackVariant> variants = DATAPACK_VARIANTS.get(entityId);
        if (variants == null) return null;
        return new VariantProvider<>() {
            @Override
            public List<VariantDef> getVariants(Mob entity) {
                return variants.stream()
                        .map(v -> new VariantDef(v.id(), v.nbt()))
                        .toList();
            }

            @Override
            public void apply(Mob entity, VariantDef def) {
                if (def.value() instanceof CompoundTag nbt) {
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
                        TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                        entity.saveWithoutId(output);

                        if (output.buildResult() instanceof CompoundTag current) {
                            current.merge(nbt);
                            entity.load(TagValueInput.create(reporter, entity.registryAccess(), current));
                        }
                    }
                }
            }

            @Override
            public VariantDef getCurrent(Mob entity) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
                    TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                    entity.saveWithoutId(output);

                    if (output.buildResult() instanceof CompoundTag entityNbt) {
                        for (DatapackVariant v : variants) {
                            if (NbtUtils.compareNbt(v.nbt(), entityNbt, true)) {
                                return new VariantDef(v.id(), v.nbt());
                            }
                        }
                    }
                }
                return new VariantDef("default", null);
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
                        .map(e -> new VariantDef(e.getKey().identifier().toString(), e.getValue()))
                        .toList();
            }

            @Override
            public void apply(Mob entity, VariantDef def) {
                if (entity instanceof VillagerDataHolder holder) {
                    VillagerData data = holder.getVillagerData();
                    holder.setVillagerData(data.withType(BuiltInRegistries.VILLAGER_TYPE.wrapAsHolder((VillagerType) def.value())));
                }
            }

            @Override
            public VariantDef getCurrent(Mob entity) {
                if (entity instanceof VillagerDataHolder holder) {
                    VillagerType type = holder.getVillagerData().type().value();
                    Identifier id = BuiltInRegistries.VILLAGER_TYPE.getKey(type);
                    return new VariantDef(id.toString(), type);
                }
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
  
                    if (!m.getReturnType().isEnum() || m.getReturnType().getSimpleName().equals("DyeColor")) {  
                        continue;  
                    }  
  
                    String suffix = name.startsWith("get") ? name.substring(3) : name.substring(2);  
                    try {  
                        Method potentialSetter = mob.getClass().getMethod("set" + suffix, m.getReturnType());  
  
                        final Method finalGetter = m;  
                        final Method finalSetter = potentialSetter;  
  
                        return new VariantProvider<>() {  
                            @Override  
                            public List<VariantDef> getVariants(Mob entity) {  
                                return Arrays.stream(finalGetter.getReturnType().getEnumConstants())  
                                        .map(e -> new VariantDef(((Enum<?>) e).name(), e))  
                                        .toList();  
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
            return providers.stream()
                    .flatMap(p -> p.getVariants(entity).stream())
                    .distinct()
                    .toList();
        }

        @Override
        public void apply(T entity, VariantDef def) {
            for (VariantProvider<T> p : providers) {
                p.apply(entity, def);
            }
        }

        @Override
        public VariantDef getCurrent(T entity) {
            for (VariantProvider<T> p : providers) {
                VariantDef current = p.getCurrent(entity);
                if (current != null && !current.id().equals("default")) return current;
            }
            return new VariantDef("default", null);
        }

        @Override
        public String getCacheKey(T entity) {
            return providers.stream().map(p -> p.getCacheKey(entity)).collect(Collectors.joining("_"));
        }
    }
}
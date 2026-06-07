package com.evandev.fieldguide.variant;

import com.evandev.fieldguide.api.variant.DatapackVariant;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.config.ServerConfig;
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
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.npc.VillagerData;
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
    private static final Map<ResourceLocation, List<DatapackVariant>> DATAPACK_VARIANTS = new HashMap<>();
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
                entity.setColor((DyeColor) def.value());
            }

            @Override
            public VariantDef getCurrent(Sheep entity) {
                return new VariantDef(entity.getColor().getName(), entity.getColor());
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
                entity.setVariant((Variant) def.value());
            }

            @Override
            public VariantDef getCurrent(Horse entity) {
                return new VariantDef(entity.getVariant().name(), entity.getVariant());
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
                entity.setVariant((Llama.Variant) def.value());
            }

            @Override
            public VariantDef getCurrent(Llama entity) {
                return new VariantDef(entity.getVariant().name(), entity.getVariant());
            }
        });
    }

    public static <T extends Mob> void registerProvider(Class<T> entityClass, VariantProvider<T> provider) {
        PROVIDERS.put(entityClass, provider);
    }

    public static void setDatapackVariants(Map<ResourceLocation, List<DatapackVariant>> variants) {
        DATAPACK_VARIANTS.clear();
        DATAPACK_VARIANTS.putAll(variants);
        VARIANT_CACHE.clear();
        ENTITY_TYPE_VARIANT_CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> VariantProvider<T> getProvider(Entity entity) {
        if (ServerConfig.get().disableVariants) return null;
        if (!(entity instanceof Mob mob)) return null;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

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
        if (VillagerDataHolder.class.isAssignableFrom(entityClass)) {
            return (VariantProvider<T>) getVillagerProvider();
        }

        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Mob.class && clazz != Object.class) {
            if (PROVIDERS.containsKey(clazz)) {
                return (VariantProvider<T>) PROVIDERS.get(clazz);
            }
            for (Class<?> iface : clazz.getInterfaces()) {
                if (PROVIDERS.containsKey(iface)) {
                    return (VariantProvider<T>) PROVIDERS.get(iface);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
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

        name = Arrays.stream(name.split("_"))
                .map(s -> s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        return Component.literal(name);
    }

    private static VariantProvider<Mob> getDatapackProvider(ResourceLocation entityId) {
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
                    CompoundTag current = new CompoundTag();
                    entity.saveWithoutId(current);
                    current.merge(nbt);
                    entity.load(current);
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
                return new VariantDef("default", null);
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
                if (entity instanceof VillagerDataHolder holder) {
                    VillagerData data = holder.getVillagerData();
                    holder.setVillagerData(data.setType((VillagerType) def.value()));
                }
            }

            @Override
            public VariantDef getCurrent(Mob entity) {
                if (entity instanceof VillagerDataHolder holder) {
                    VillagerType type = holder.getVillagerData().getType();
                    ResourceLocation id = BuiltInRegistries.VILLAGER_TYPE.getKey(type);
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
}
package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.Constants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

//? if <26.2 {
import net.minecraft.advancements.CriterionTrigger;
//?} else {
/*import net.minecraft.advancements.triggers.CriterionTrigger;
*///?}

//? if fabric {
/*import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
*///?} else {
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//?}

//? if fabric && <26.1 {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?} else if fabric {
/*import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
*///?}

public class RegistryHelper {
    //? if neoforge {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Constants.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES = DeferredRegister.create(Registries.TRIGGER_TYPE, Constants.MOD_ID);
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> TAB_ENTRIES = new HashMap<>();

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        DATA_COMPONENTS.register(bus);
        TRIGGER_TYPES.register(bus);
        bus.addListener(RegistryHelper::onBuildCreativeTabs);
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        List<Supplier<? extends ItemLike>> items = TAB_ENTRIES.get(event.getTabKey());

        if (items != null) {
            for (Supplier<? extends ItemLike> item : items) {
                event.accept(item.get());
            }
        }
    }
    //?} else {
    /*private static <V, T extends V> Supplier<T> register(Registry<V> registry, String name, T value) {
        T registered = Registry.register(registry, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), value);
        return () -> registered;
    }
    *///?}

    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        //? if fabric {
        /*return register(BuiltInRegistries.ITEM, name, item.get());
        *///?} else {
        return ITEMS.register(name, item);
        //?}
    }

    public <T> Supplier<DataComponentType<T>> registerComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        //? if fabric {
        /*return register(BuiltInRegistries.DATA_COMPONENT_TYPE, name, builderOperator.apply(DataComponentType.builder()).build());
        *///?} else {
        return DATA_COMPONENTS.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
        //?}
    }

    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterion(String name, Supplier<T> trigger) {
        //? if fabric {
        /*return register(BuiltInRegistries.TRIGGER_TYPES, name, trigger.get());
        *///?} else {
        return TRIGGER_TYPES.register(name, trigger);
        //?}
    }

    public void registerToTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        //? if fabric && <26.1 {
        /*ItemGroupEvents.modifyEntriesEvent(tab).register(content -> content.accept(item.get()));
        *///?} else if fabric {
        /*CreativeModeTabEvents.modifyOutputEvent(tab).register(content -> content.accept(item.get()));
        *///?} else {
        TAB_ENTRIES.computeIfAbsent(tab, k -> new ArrayList<>()).add(item);
        //?}
    }
}

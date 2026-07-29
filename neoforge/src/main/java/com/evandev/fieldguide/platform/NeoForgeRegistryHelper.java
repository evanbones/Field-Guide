package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.services.IRegistryHelper;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class NeoForgeRegistryHelper implements IRegistryHelper {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Constants.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES = DeferredRegister.create(Registries.TRIGGER_TYPE, Constants.MOD_ID);
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> TAB_ENTRIES = new HashMap<>();

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        DATA_COMPONENTS.register(bus);
        TRIGGER_TYPES.register(bus);
        bus.addListener(NeoForgeRegistryHelper::onBuildCreativeTabs);
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        List<Supplier<? extends ItemLike>> items = TAB_ENTRIES.get(event.getTabKey());

        if (items != null) {
            for (Supplier<? extends ItemLike> item : items) {
                event.accept(item.get());
            }
        }
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        return ITEMS.register(name, item);
    }

    @Override
    public <T> Supplier<DataComponentType<T>> registerComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENTS.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    @Override
    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterion(String name, Supplier<T> trigger) {
        return TRIGGER_TYPES.register(name, trigger);
    }

    @Override
    public void registerToTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> itemSupplier) {
        TAB_ENTRIES.computeIfAbsent(tab, k -> new ArrayList<>()).add(itemSupplier);
    }
}
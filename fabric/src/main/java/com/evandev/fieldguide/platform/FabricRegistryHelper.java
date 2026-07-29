package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.services.IRegistryHelper;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> itemSupplier) {
        T item = Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), itemSupplier.get());
        return () -> item;
    }

    @Override
    public <T> Supplier<DataComponentType<T>> registerComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        DataComponentType<T> type = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), builderOperator.apply(DataComponentType.builder()).build());
        return () -> type;
    }

    @Override
    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterion(String name, Supplier<T> triggerSupplier) {
        T trigger = Registry.register(BuiltInRegistries.TRIGGER_TYPES, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), triggerSupplier.get());
        return () -> trigger;
    }

    @Override
    public void registerToTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> itemSupplier) {
        CreativeModeTabEvents.modifyOutputEvent(tab).register(content -> content.accept(itemSupplier.get()));
    }
}

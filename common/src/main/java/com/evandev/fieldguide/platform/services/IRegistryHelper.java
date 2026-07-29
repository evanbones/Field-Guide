package com.evandev.fieldguide.platform.services;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface IRegistryHelper {

    /**
     * Registers an item.
     *
     * @param name The name of the item.
     * @param item The item supplier.
     * @return The registered item supplier.
     */
    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item);

    /**
     * Registers a data component type.
     *
     * @param name The name of the component.
     * @param builderOperator The builder operator.
     * @return The registered component type supplier.
     */
    <T> Supplier<DataComponentType<T>> registerComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator);

    /**
     * Registers a criterion trigger.
     *
     * @param name The name of the trigger.
     * @param trigger The trigger supplier.
     * @return The registered trigger supplier.
     */
    <T extends CriterionTrigger<?>> Supplier<T> registerCriterion(String name, Supplier<T> trigger);

    /**
     * Registers an item to a creative tab.
     *
     * @param tab  The creative tab.
     * @param item The item supplier.
     */
    void registerToTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item);
}

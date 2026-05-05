package com.evandev.fieldguide.platform.services;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Gets the configuration directory for the current platform.
     *
     * @return The path to the config directory.
     */
    Path getConfigDirectory();

    /**
     * Applies platform-specific dummy entity configurations.
     */
    default void applyMixedLitterCompat(Entity entity) {
    }

    /**
     * Checks if the player has a spyglass equipped in a platform-specific slot (e.g., Curios, Trinkets)
     *
     * @param player The player to check.
     * @return True if a spyglass is equipped in a special slot, false otherwise.
     */
    boolean hasSpyglassEquipped(Player player);
}
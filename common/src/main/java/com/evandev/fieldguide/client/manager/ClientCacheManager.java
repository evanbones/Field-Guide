package com.evandev.fieldguide.client.manager;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.Services;
import com.google.common.hash.Hashing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientCacheManager {
    private static final Path CACHE_BASE_DIR = Services.PLATFORM.getConfigDirectory().resolve("../fieldguide_cache");
    private static String currentSessionKey = null;

    public static void init() {
        try {
            Files.createDirectories(CACHE_BASE_DIR);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create cache directory", e);
        }
    }

    private static String getSessionKey() {
        if (currentSessionKey != null) return currentSessionKey;

        Minecraft mc = Minecraft.getInstance();
        String rawKey;
        if (mc.isLocalServer() && mc.getSingleplayerServer() != null) {
            rawKey = "singleplayer_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            ServerData serverData = mc.getCurrentServer();
            rawKey = "multiplayer_" + (serverData != null ? serverData.ip : "unknown");
        }

        currentSessionKey = Hashing.sha256().hashString(rawKey, StandardCharsets.UTF_8).toString().substring(0, 16);
        return currentSessionKey;
    }

    private static Path getSessionDir() {
        Path sessionDir = CACHE_BASE_DIR.resolve(getSessionKey());
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException ignored) {
        }
        return sessionDir;
    }

    private static HolderLookup.Provider getRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        } else if (mc.getConnection() != null) {
            return mc.getConnection().registryAccess();
        }
        throw new IllegalStateException("Registry access is not available yet!");
    }

    public static void saveAllDrops(Map<ResourceLocation, List<ItemStack>> allDrops) {
        if (allDrops == null || allDrops.isEmpty()) return;

        HolderLookup.Provider provider = getRegistryAccess();
        Path dropFile = getSessionDir().resolve("drops.nbt");
        CompoundTag root = loadNbt(dropFile);

        for (Map.Entry<ResourceLocation, List<ItemStack>> entry : allDrops.entrySet()) {
            ListTag list = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                list.add(stack.saveOptional(provider));
            }
            root.put(entry.getKey().toString(), list);
        }
        saveNbt(dropFile, root);
    }

    public static List<ItemStack> loadDrops(ResourceLocation entryId) {
        Path dropFile = getSessionDir().resolve("drops.nbt");
        CompoundTag root = loadNbt(dropFile);

        if (root.contains(entryId.toString(), Tag.TAG_LIST)) {
            HolderLookup.Provider provider = getRegistryAccess();
            ListTag list = root.getList(entryId.toString(), Tag.TAG_COMPOUND);
            List<ItemStack> drops = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                drops.add(ItemStack.parseOptional(provider, list.getCompound(i)));
            }
            return drops;
        }
        return null;
    }

    public static void saveBiomes(ResourceLocation entryId, List<ResourceLocation> biomes) {
        if (biomes == null) return;

        Path biomeFile = getSessionDir().resolve("biomes.nbt");
        CompoundTag root = loadNbt(biomeFile);
        ListTag list = new ListTag();
        for (ResourceLocation biome : biomes) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", biome.toString());
            list.add(tag);
        }
        root.put(entryId.toString(), list);
        saveNbt(biomeFile, root);
    }

    public static List<ResourceLocation> loadBiomes(ResourceLocation entryId) {
        Path biomeFile = getSessionDir().resolve("biomes.nbt");
        CompoundTag root = loadNbt(biomeFile);
        if (root.contains(entryId.toString(), Tag.TAG_LIST)) {
            ListTag list = root.getList(entryId.toString(), Tag.TAG_COMPOUND);
            List<ResourceLocation> biomes = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                biomes.add(ResourceLocation.parse(list.getCompound(i).getString("id")));
            }
            return biomes;
        }
        return null;
    }

    private static CompoundTag loadNbt(Path path) {
        if (Files.exists(path)) {
            try {
                return NbtIo.read(path);
            } catch (IOException e) {
                Constants.LOG.error("Failed to read NBT cache from {}", path, e);
            }
        }
        return new CompoundTag();
    }

    private static void saveNbt(Path path, CompoundTag tag) {
        try {
            NbtIo.write(tag, path);
        } catch (IOException e) {
            Constants.LOG.error("Failed to write NBT cache to {}", path, e);
        }
    }

    public static void clearDiskCache() {
        try {
            Path sessionDir = getSessionDir();
            Files.deleteIfExists(sessionDir.resolve("drops.nbt"));
            Files.deleteIfExists(sessionDir.resolve("biomes.nbt"));
        } catch (IOException e) {
            Constants.LOG.error("Failed to delete cache files", e);
        }
    }

    public static void onWorldUnload() {
        currentSessionKey = null;
    }
}
package com.evandev.fieldguide.server.progress;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FieldGuideProgressManager {
    private static final FieldGuideProgressManager NOOP = new FieldGuideProgressManager();
    private static FieldGuideProgressManager INSTANCE = NOOP;

    private MinecraftServer server;
    private Path progressDir;
    private final Map<UUID, PlayerFieldGuideProgress> playerProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Long>> recentlyScannedEntities = new HashMap<>();

    private FieldGuideProgressManager() {
    }

    private FieldGuideProgressManager(MinecraftServer server) {
        this.server = server;
        this.progressDir = server.getWorldPath(LevelResource.ROOT).resolve("fieldguide_progress");
    }

    public void recordScan(ServerPlayer player, int entityId) {
        recentlyScannedEntities.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(entityId, System.currentTimeMillis());
    }

    public boolean wasRecentlyScanned(ServerPlayer player, int entityId) {
        Map<Integer, Long> scans = recentlyScannedEntities.get(player.getUUID());
        if (scans != null) {
            Long time = scans.get(entityId);
            if (time != null) {
                return (System.currentTimeMillis() - time) < 10000; // 10 seconds
            }
        }
        return false;
    }

    public static void init(MinecraftServer server) {
        INSTANCE = new FieldGuideProgressManager(server);
        INSTANCE.migrateLegacyProgress();
    }

    public static void shutdown() {
        if (INSTANCE != NOOP) {
            INSTANCE.saveAll();
            INSTANCE.playerProgress.clear();
            INSTANCE = NOOP;
        }
    }

    public static FieldGuideProgressManager getInstance() {
        return INSTANCE;
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (this == NOOP) return;
        UUID uuid = player.getUUID();
        PlayerFieldGuideProgress progress = new PlayerFieldGuideProgress(uuid, progressDir);
        progress.load();
        progress.markForFullSync();
        playerProgress.put(uuid, progress);
    }

    private void migrateLegacyProgress() {
        GameProfile profile = server.getSingleplayerProfile();
        if (profile == null) return;

        Path legacyFile = server.getWorldPath(LevelResource.ROOT).resolve("fieldguide_data").resolve("progress.dat");
        if (!Files.exists(legacyFile)) return;

        try {
            Files.createDirectories(progressDir);
            Files.move(legacyFile, progressDir.resolve(profile.getId() + ".json"));
            Constants.LOG.info("Migrated legacy field guide progress for {}", profile.getName());
        } catch (FileAlreadyExistsException ignored) {
        } catch (Exception e) {
            Constants.LOG.error("Failed to migrate legacy field guide progress for {}", profile.getName(), e);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerFieldGuideProgress progress = playerProgress.remove(uuid);
        if (progress != null) {
            progress.save();
        }
    }

    public void tick() {
        for (Map.Entry<UUID, PlayerFieldGuideProgress> entry : playerProgress.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().flushDirty(player);
            }
        }
    }

    public void save(ServerPlayer player) {
        PlayerFieldGuideProgress progress = playerProgress.get(player.getUUID());
        if (progress != null) {
            progress.save();
        }
    }

    public void saveAll() {
        for (PlayerFieldGuideProgress progress : playerProgress.values()) {
            progress.save();
        }
    }

    public PlayerFieldGuideProgress getProgress(ServerPlayer player) {
        return playerProgress.get(player.getUUID());
    }

    public PlayerFieldGuideProgress getProgress(UUID uuid) {
        return playerProgress.get(uuid);
    }

    public boolean isValidEntry(ResourceLocation entryId) {
        return ServerFieldGuideManager.getInstance().hasEntry(entryId);
    }

    public boolean isKillToUnlock(ResourceLocation entryId) {
        return ServerFieldGuideManager.getInstance().isKillToUnlock(entryId);
    }

    public boolean isEatToUnlock(ResourceLocation entryId) {
        return ServerFieldGuideManager.getInstance().isEatToUnlock(entryId);
    }
}

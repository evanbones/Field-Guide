package com.evandev.fieldguide.server.progress;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.ProgressUpdatePacket;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

public class PlayerFieldGuideProgress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final UUID playerUUID;
    private final Path savePath;

    private final Set<String> unlockedEntries = new HashSet<>();
    private final Set<String> seenEntries = new HashSet<>();
    private final Map<String, Long> discoveryTimes = new HashMap<>();
    private final Map<String, Long> discoveryGameTimes = new HashMap<>();

    private final Map<String, String> customNames = new HashMap<>();
    private final Map<String, String> customDescriptions = new HashMap<>();
    private final Map<String, String> entryPhotographs = new HashMap<>();
    private final Map<String, String> selectedVariants = new HashMap<>();
    private final List<JournalPageData> journalPages = new ArrayList<>();

    private final Set<String> pendingUnlocks = new LinkedHashSet<>();
    private final Set<String> pendingRevokes = new LinkedHashSet<>();
    private final Set<String> pendingSeen = new LinkedHashSet<>();
    private final Set<String> pendingEntryResync = new LinkedHashSet<>();
    private boolean pendingFullSync = false;

    private String journalTitle = null;
    private boolean dirty = false;

    public PlayerFieldGuideProgress(UUID playerUUID, Path progressDir) {
        this.playerUUID = playerUUID;
        this.savePath = progressDir.resolve(playerUUID.toString() + ".json");
    }

    private static <T> List<T> chunkAt(List<T> list, int index, int length) {
        return list.subList(Math.min(index, list.size()), Math.min(index + length, list.size()));
    }

    public void checkDefaultUnlocks(ServerPlayer player) {
        for (ResourceLocation entryId : ServerFieldGuideManager.getInstance().getAllEntryIds()) {
            EntryUnlockData unlockData = ServerFieldGuideManager.getInstance().getUnlockData(entryId);
            if (unlockData.unlockedByDefault() && canUnlock(entryId)) {
                unlock(player, entryId, null, false);
            }
        }
    }

    public boolean canUnlock(ResourceLocation entryId) {
        EntryUnlockData unlockData = ServerFieldGuideManager.getInstance().getUnlockData(entryId);
        for (ResourceLocation prereq : unlockData.prerequisites()) {
            if (!isUnlocked(prereq)) return false;
        }
        return true;
    }

    public void tryUnlock(ServerPlayer player, ResourceLocation triggeredId, String variantId, EntryUnlockData.UnlockTrigger trigger) {
        tryUnlockDirect(player, triggeredId, variantId, trigger);
        for (ResourceLocation entryId : ServerFieldGuideManager.getInstance().getEntriesTriggeredBy(triggeredId)) {
            tryUnlockDirect(player, entryId, null, trigger);
        }
    }

    private void tryUnlockDirect(ServerPlayer player, ResourceLocation entryId, String variantId, EntryUnlockData.UnlockTrigger trigger) {
        if (trigger == EntryUnlockData.UnlockTrigger.OBTAIN && ServerConfig.get().disableObtainUnlocks) return;
        if (!ServerFieldGuideManager.getInstance().hasEntry(entryId)) return;

        if (isUnlocked(entryId)) {
            if (variantId != null && !variantId.isEmpty()) {
                unlock(player, entryId, variantId, true);
            }
            return;
        }

        if (!canUnlock(entryId)) return;

        EntryUnlockData unlockData = ServerFieldGuideManager.getInstance().getUnlockData(entryId);
        boolean isDefaultScan = unlockData.triggers().isEmpty() && trigger == EntryUnlockData.UnlockTrigger.SCAN;

        if (isDefaultScan || unlockData.triggers().contains(trigger)) {
            unlock(player, entryId, variantId, true);
        }
    }

    public void unlock(ServerPlayer player, ResourceLocation entryId, String variantId, boolean grantXp) {
        ResourceLocation canonicalId = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        if (canonicalId == null) canonicalId = entryId;
        String id = canonicalId.toString();
        boolean newlyUnlocked = false;

        if (!EntryResolver.isUnlocked(unlockedEntries, canonicalId, canonicalId, null) && unlockedEntries.add(id)) {
            discoveryTimes.put(id, System.currentTimeMillis());
            discoveryGameTimes.put(id, player.serverLevel().dayTime());
            pendingUnlocks.add(id);
            pendingRevokes.remove(id);
            newlyUnlocked = true;
            UnlockRewards.grant(player, canonicalId, grantXp);
            FieldGuideTriggers.ENTRY_UNLOCKED.get().trigger(player, canonicalId);
            Services.PLATFORM.onEntryUnlocked(player, canonicalId, null, true, this);

            ResourceLocation categoryId = ServerFieldGuideManager.getInstance().getCategoryForEntryId(canonicalId);
            if (categoryId != null) {
                Set<ResourceLocation> categoryEntries = ServerFieldGuideManager.getInstance().getEntryIdsForCategory(categoryId);
                if (!categoryEntries.isEmpty() && categoryEntries.stream().allMatch(e -> isUnlocked(e.toString()))) {
                    FieldGuideTriggers.CATEGORY_COMPLETED.get().trigger(player, categoryId);
                    Services.PLATFORM.onCategoryCompleted(player, categoryId, this);
                }
            }
        }

        if (variantId != null && !variantId.isEmpty()) {
            String fullVariantId = id + "#" + variantId;
            if (!EntryResolver.isUnlocked(unlockedEntries, canonicalId, canonicalId, variantId) && unlockedEntries.add(fullVariantId)) {
                pendingUnlocks.add(fullVariantId);
                pendingRevokes.remove(fullVariantId);
                newlyUnlocked = true;
                Services.PLATFORM.onEntryUnlocked(player, canonicalId, variantId, false, this);
            }
        }

        if (newlyUnlocked) {
            dirty = true;
            checkDefaultUnlocks(player);
        }
    }

    public boolean revoke(String entryId) {
        boolean removed = false;
        List<String> toRemove = new ArrayList<>();
        ResourceLocation idLoc = null;
        ResourceLocation canonicalLoc = null;
        try {
            idLoc = ResourceLocation.parse(entryId);
            canonicalLoc = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(idLoc);
        } catch (Exception ignored) {
        }

        Set<String> candidates = new LinkedHashSet<>(unlockedEntries);
        candidates.addAll(customNames.keySet());
        candidates.addAll(customDescriptions.keySet());
        candidates.addAll(entryPhotographs.keySet());
        candidates.addAll(selectedVariants.keySet());

        for (String id : candidates) {
            if (EntryResolver.matchesStoredEntry(id, entryId, canonicalLoc, idLoc)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            boolean wasUnlocked = unlockedEntries.remove(id);
            boolean hadData = entryPhotographs.remove(id) != null
                    | customNames.remove(id) != null
                    | customDescriptions.remove(id) != null
                    | selectedVariants.remove(id) != null;

            if (wasUnlocked || hadData) {
                seenEntries.remove(id);
                discoveryTimes.remove(id);
                discoveryGameTimes.remove(id);
                pendingRevokes.add(id);
                pendingUnlocks.remove(id);
                removed = true;
            }
        }

        if (removed) {
            dirty = true;
        }
        return removed;
    }

    public List<String> getUnlockedVariants(String entryId) {
        List<String> variants = new ArrayList<>();
        ResourceLocation idLoc = null;
        ResourceLocation canonicalLoc = null;
        try {
            idLoc = ResourceLocation.parse(entryId);
            canonicalLoc = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(idLoc);
        } catch (Exception ignored) {
        }

        for (String id : unlockedEntries) {
            int hashIdx = id.indexOf('#');
            if (hashIdx == -1) continue;
            String base = id.substring(0, hashIdx);
            String variant = id.substring(hashIdx + 1);

            if (EntryResolver.matchesStoredEntry(base, entryId, canonicalLoc, idLoc) && !variants.contains(variant)) {
                variants.add(variant);
            }
        }
        return variants;
    }

    public String getCustomName(String entryId) {
        return customNames.get(entryId);
    }

    public String getCustomDescription(String entryId) {
        return customDescriptions.get(entryId);
    }

    public String getEntryPhotograph(String entryId) {
        return entryPhotographs.get(entryId);
    }

    public String getSelectedVariant(String entryId) {
        return selectedVariants.get(entryId);
    }

    public long getDiscoveryTime(String entryId) {
        return discoveryTimes.getOrDefault(entryId, 0L);
    }

    public long getDiscoveryGameTime(String entryId) {
        return discoveryGameTimes.getOrDefault(entryId, 0L);
    }

    public void setCustomName(String entryId, String name) {
        if (name == null || name.isEmpty()) {
            customNames.remove(entryId);
        } else {
            customNames.put(entryId, name);
        }
        pendingEntryResync.add(entryId);
        dirty = true;
    }

    public void setCustomDescription(String entryId, String description) {
        if (description == null || description.isEmpty()) {
            customDescriptions.remove(entryId);
        } else {
            customDescriptions.put(entryId, description);
        }
        pendingEntryResync.add(entryId);
        dirty = true;
    }

    public void setEntryPhotograph(String entryId, String photograph) {
        if (photograph == null || photograph.isEmpty()) {
            entryPhotographs.remove(entryId);
        } else {
            entryPhotographs.put(entryId, photograph);
        }
        pendingEntryResync.add(entryId);
        dirty = true;
    }

    public void setSelectedVariant(String entryId, String variantId) {
        if (variantId == null || variantId.isEmpty()) {
            selectedVariants.remove(entryId);
        } else {
            selectedVariants.put(entryId, variantId);
        }
        pendingEntryResync.add(entryId);
        dirty = true;
    }

    public void setDiscoveryTime(String entryId, long time) {
        discoveryTimes.put(entryId, time);
        dirty = true;
    }

    public void setDiscoveryGameTime(String entryId, long gameTime) {
        discoveryGameTimes.put(entryId, gameTime);
        dirty = true;
    }

    public boolean revoke(ResourceLocation entryId) {
        return revoke(entryId.toString());
    }

    public void revokeAll() {
        if (!unlockedEntries.isEmpty()) {
            unlockedEntries.clear();
            seenEntries.clear();
            discoveryTimes.clear();
            discoveryGameTimes.clear();
            entryPhotographs.clear();
            customNames.clear();
            customDescriptions.clear();
            selectedVariants.clear();
            pendingUnlocks.clear();
            pendingRevokes.clear();
            pendingSeen.clear();
            pendingFullSync = true;
            dirty = true;
        }
    }

    public void markSeen(String entryId) {
        if (unlockedEntries.contains(entryId) && seenEntries.add(entryId)) {
            pendingSeen.add(entryId);
            dirty = true;
        }
    }

    public boolean isUnlocked(String entryId) {
        if (entryId == null) return false;
        try {
            ResourceLocation id = ResourceLocation.parse(entryId);
            ResourceLocation canonical = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(id);
            return EntryResolver.isUnlocked(unlockedEntries, id, canonical, null);
        } catch (Exception e) {
            return unlockedEntries.contains(entryId);
        }
    }

    public boolean isUnlocked(String entryId, String variantId) {
        if (entryId == null) return false;
        try {
            ResourceLocation id = ResourceLocation.parse(entryId);
            ResourceLocation canonical = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(id);
            return EntryResolver.isUnlocked(unlockedEntries, id, canonical, variantId);
        } catch (Exception e) {
            String suffix = (variantId != null && !variantId.isEmpty()) ? "#" + variantId : "";
            return unlockedEntries.contains(entryId + suffix);
        }
    }

    public boolean isUnlocked(ResourceLocation entryId) {
        if (entryId == null) return false;
        ResourceLocation canonical = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        return EntryResolver.isUnlocked(unlockedEntries, entryId, canonical, null);
    }

    public boolean isUnlocked(ResourceLocation entryId, String variantId) {
        if (entryId == null) return false;
        ResourceLocation canonical = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        return EntryResolver.isUnlocked(unlockedEntries, entryId, canonical, variantId);
    }

    public Set<String> getUnlockedEntries() {
        return Collections.unmodifiableSet(unlockedEntries);
    }

    public int getUnlockedEntryCount() {
        return (int) unlockedEntries.stream().filter(id -> id.indexOf('#') == -1).count();
    }

    public void setJournalTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            this.journalTitle = null;
        } else {
            this.journalTitle = title;
        }
        dirty = true;
    }

    public void setJournalPages(List<JournalPageData> pages) {
        this.journalPages.clear();
        this.journalPages.addAll(pages);
        dirty = true;
    }

    public void markForFullSync() {
        this.pendingFullSync = true;
    }

    public void markEntryForResync(String entryId) {
        pendingEntryResync.add(entryId);
    }

    public void flushDirty(ServerPlayer player) {
        if (pendingFullSync) {
            sendFullSync(player);
            pendingFullSync = false;
            pendingUnlocks.clear();
            pendingRevokes.clear();
            pendingSeen.clear();
            pendingEntryResync.clear();
            return;
        }

        if (!pendingUnlocks.isEmpty() || !pendingRevokes.isEmpty() || !pendingSeen.isEmpty() || !pendingEntryResync.isEmpty()) {
            sendDelta(player);
            pendingUnlocks.clear();
            pendingRevokes.clear();
            pendingSeen.clear();
            pendingEntryResync.clear();
        }
    }

    private void sendFullSync(ServerPlayer player) {
        int chunkSize = 256;
        List<String> allUnlocked = new ArrayList<>(unlockedEntries);
        int iterations = Math.max(1, allUnlocked.size());

        Map<String, List<String>> allTriggers = new HashMap<>();
        for (ResourceLocation resId : ServerFieldGuideManager.getInstance().getAllEntryIds()) {
            EntryUnlockData unlockData = ServerFieldGuideManager.getInstance().getUnlockData(resId);
            List<String> triggers = unlockData.triggers().stream()
                    .map(Enum::name)
                    .toList();

            if (!triggers.isEmpty()) {
                allTriggers.put(resId.toString(), triggers);
            }
        }

        boolean first = true;
        for (int i = 0; i < iterations; i += chunkSize) {
            List<String> unlockedChunk = chunkAt(allUnlocked, i, chunkSize);

            List<String> seenChunk = new ArrayList<>();
            Map<String, Long> times = new HashMap<>();
            Map<String, Long> gameTimes = new HashMap<>();
            Map<String, String> names = new HashMap<>();
            Map<String, String> descs = new HashMap<>();
            Map<String, String> photos = new HashMap<>();
            Map<String, String> variants = new HashMap<>();
            for (String id : unlockedChunk) {
                if (seenEntries.contains(id)) seenChunk.add(id);
                if (discoveryTimes.containsKey(id)) times.put(id, discoveryTimes.get(id));
                if (discoveryGameTimes.containsKey(id)) gameTimes.put(id, discoveryGameTimes.get(id));
                if (customNames.containsKey(id)) names.put(id, customNames.get(id));
                if (customDescriptions.containsKey(id)) descs.put(id, customDescriptions.get(id));
                if (entryPhotographs.containsKey(id)) photos.put(id, entryPhotographs.get(id));
                if (selectedVariants.containsKey(id)) variants.put(id, selectedVariants.get(id));
            }

            Services.NETWORK.sendToPlayer(
                    new ProgressUpdatePacket.Builder()
                            .reset(first)
                            .silent(true)
                            .unlocked(unlockedChunk)
                            .seen(seenChunk)
                            .discoveryTimes(times)
                            .discoveryGameTimes(gameTimes)
                            .customNames(names)
                            .customDescriptions(descs)
                            .selectedVariants(variants)
                            .killedOnly(ServerFieldGuideManager.getInstance().getAllEntryIds().stream()
                                    .filter(id -> ServerFieldGuideManager.getInstance().getUnlockData(id).triggers().contains(EntryUnlockData.UnlockTrigger.KILL))
                                    .map(ResourceLocation::toString)
                                    .toList())
                            .eatenOnly(ServerFieldGuideManager.getInstance().getAllEntryIds().stream()
                                    .filter(id -> ServerFieldGuideManager.getInstance().getUnlockData(id).triggers().contains(EntryUnlockData.UnlockTrigger.EAT))
                                    .map(ResourceLocation::toString)
                                    .toList())
                            .entryTriggers(first ? allTriggers : Collections.emptyMap())
                            .build(),
                    player
            );
            first = false;

            if (!photos.isEmpty()) {
                Services.NETWORK.sendToPlayer(
                        new ProgressUpdatePacket.Builder()
                                .silent(true)
                                .entryPhotographs(photos)
                                .build(),
                        player
                );
            }
        }

        sendOrphanedEntryData(player, allUnlocked, chunkSize);

        Services.NETWORK.sendToPlayer(
                new ProgressUpdatePacket.Builder()
                        .silent(true)
                        .journalTitle(journalTitle != null ? journalTitle : "")
                        .journalPages(new ArrayList<>(journalPages))
                        .build(),
                player);
    }

    private void sendOrphanedEntryData(ServerPlayer player, List<String> alreadySent, int chunkSize) {
        Set<String> sent = new HashSet<>(alreadySent);
        Set<String> remaining = new LinkedHashSet<>();
        for (Map<String, String> map : List.of(customNames, customDescriptions, entryPhotographs, selectedVariants)) {
            for (String id : map.keySet()) {
                if (!sent.contains(id)) remaining.add(id);
            }
        }
        if (remaining.isEmpty()) return;

        List<String> ids = new ArrayList<>(remaining);
        for (int i = 0; i < ids.size(); i += chunkSize) {
            List<String> chunk = chunkAt(ids, i, chunkSize);

            Map<String, String> names = new HashMap<>();
            Map<String, String> descs = new HashMap<>();
            Map<String, String> photos = new HashMap<>();
            Map<String, String> variants = new HashMap<>();
            for (String id : chunk) {
                if (customNames.containsKey(id)) names.put(id, customNames.get(id));
                if (customDescriptions.containsKey(id)) descs.put(id, customDescriptions.get(id));
                if (entryPhotographs.containsKey(id)) photos.put(id, entryPhotographs.get(id));
                if (selectedVariants.containsKey(id)) variants.put(id, selectedVariants.get(id));
            }

            Services.NETWORK.sendToPlayer(
                    new ProgressUpdatePacket.Builder()
                            .silent(true)
                            .customNames(names)
                            .customDescriptions(descs)
                            .selectedVariants(variants)
                            .build(),
                    player
            );

            if (!photos.isEmpty()) {
                Services.NETWORK.sendToPlayer(
                        new ProgressUpdatePacket.Builder()
                                .silent(true)
                                .entryPhotographs(photos)
                                .build(),
                        player
                );
            }
        }
    }

    private void sendDelta(ServerPlayer player) {
        int chunkSize = 1024;
        List<String> allUnlocks = new ArrayList<>(pendingUnlocks);
        List<String> revoked = new ArrayList<>(pendingRevokes);
        List<String> seen = new ArrayList<>(pendingSeen);
        int iterations = Math.max(1, Math.max(allUnlocks.size(), Math.max(revoked.size(), seen.size())));

        Map<String, String> entryNames = new HashMap<>();
        Map<String, String> entryDescs = new HashMap<>();
        Map<String, String> entryPhotos = new HashMap<>();
        Map<String, String> entryVariants = new HashMap<>();
        for (String id : pendingEntryResync) {
            entryNames.put(id, customNames.getOrDefault(id, ""));
            entryDescs.put(id, customDescriptions.getOrDefault(id, ""));
            entryPhotos.put(id, entryPhotographs.getOrDefault(id, ""));
            entryVariants.put(id, selectedVariants.getOrDefault(id, ""));
        }

        boolean first = true;
        for (int i = 0; i < iterations; i += chunkSize) {
            List<String> unlockChunk = chunkAt(allUnlocks, i, chunkSize);
            List<String> revokedChunk = chunkAt(revoked, i, chunkSize);
            List<String> seenChunk = chunkAt(seen, i, chunkSize);

            Map<String, Long> unlockTimes = new HashMap<>();
            Map<String, Long> unlockGameTimes = new HashMap<>();
            for (String id : unlockChunk) {
                if (discoveryTimes.containsKey(id)) unlockTimes.put(id, discoveryTimes.get(id));
                if (discoveryGameTimes.containsKey(id)) unlockGameTimes.put(id, discoveryGameTimes.get(id));
            }

            Services.NETWORK.sendToPlayer(
                    new ProgressUpdatePacket.Builder()
                            .unlocked(unlockChunk)
                            .revoked(revokedChunk)
                            .seen(seenChunk)
                            .discoveryTimes(unlockTimes)
                            .discoveryGameTimes(unlockGameTimes)
                            .customNames(first ? entryNames : Collections.emptyMap())
                            .customDescriptions(first ? entryDescs : Collections.emptyMap())
                            .entryPhotographs(first ? entryPhotos : Collections.emptyMap())
                            .selectedVariants(first ? entryVariants : Collections.emptyMap())
                            .build(),
                    player
            );
            first = false;
        }
    }

    public void load() {
        try (BufferedReader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;

            if (json.has("unlocked")) {
                for (JsonElement e : json.getAsJsonArray("unlocked")) {
                    unlockedEntries.add(e.getAsString());
                }
            }
            if (json.has("seen")) {
                for (JsonElement e : json.getAsJsonArray("seen")) {
                    seenEntries.add(e.getAsString());
                }
            }
            if (json.has("times")) {
                json.getAsJsonObject("times").entrySet().forEach(
                        e -> discoveryTimes.put(e.getKey(), e.getValue().getAsLong())
                );
            }
            if (json.has("gameTimes")) {
                json.getAsJsonObject("gameTimes").entrySet().forEach(
                        e -> discoveryGameTimes.put(e.getKey(), e.getValue().getAsLong())
                );
            }
            if (json.has("customNames")) {
                json.getAsJsonObject("customNames").entrySet().forEach(
                        e -> customNames.put(e.getKey(), e.getValue().getAsString())
                );
            }
            if (json.has("customDescriptions")) {
                json.getAsJsonObject("customDescriptions").entrySet().forEach(
                        e -> customDescriptions.put(e.getKey(), e.getValue().getAsString())
                );
            }
            if (json.has("entryPhotographs")) {
                json.getAsJsonObject("entryPhotographs").entrySet().forEach(
                        e -> entryPhotographs.put(e.getKey(), e.getValue().getAsString())
                );
            }
            if (json.has("selectedVariants")) {
                json.getAsJsonObject("selectedVariants").entrySet().forEach(
                        e -> selectedVariants.put(e.getKey(), e.getValue().getAsString())
                );
            }
            if (json.has("journalTitle")) {
                String loaded = json.get("journalTitle").getAsString();
                if (!loaded.isEmpty() && !loaded.equals("My Field Guide")) {
                    journalTitle = loaded;
                } else {
                    journalTitle = null;
                }
            }
            if (json.has("journalPages")) {
                journalPages.clear();
                for (JsonElement e : json.getAsJsonArray("journalPages")) {
                    JsonObject obj = e.getAsJsonObject();
                    journalPages.add(new JournalPageData(
                            obj.has("title") ? obj.get("title").getAsString() : "",
                            obj.has("content") ? obj.get("content").getAsString() : "",
                            obj.has("timestamp") ? obj.get("timestamp").getAsLong() : System.currentTimeMillis()
                    ));
                }
            }
        } catch (NoSuchFileException ignored) {
        } catch (Exception e) {
            Constants.LOG.error("Failed to load field guide progress for {}", playerUUID, e);
        }
    }

    public void save() {
        if (!dirty) return;

        try {
            Files.createDirectories(savePath.getParent());

            JsonObject json = new JsonObject();

            JsonArray uArr = new JsonArray();
            unlockedEntries.forEach(uArr::add);
            json.add("unlocked", uArr);

            JsonArray sArr = new JsonArray();
            seenEntries.forEach(sArr::add);
            json.add("seen", sArr);

            JsonObject timesObj = new JsonObject();
            discoveryTimes.forEach(timesObj::addProperty);
            json.add("times", timesObj);

            JsonObject gameTimesObj = new JsonObject();
            discoveryGameTimes.forEach(gameTimesObj::addProperty);
            json.add("gameTimes", gameTimesObj);

            JsonObject namesObj = new JsonObject();
            customNames.forEach(namesObj::addProperty);
            json.add("customNames", namesObj);

            JsonObject descsObj = new JsonObject();
            customDescriptions.forEach(descsObj::addProperty);
            json.add("customDescriptions", descsObj);

            JsonObject photosObj = new JsonObject();
            entryPhotographs.forEach(photosObj::addProperty);
            json.add("entryPhotographs", photosObj);

            JsonObject variantsObj = new JsonObject();
            selectedVariants.forEach(variantsObj::addProperty);
            json.add("selectedVariants", variantsObj);

            if (journalTitle != null) {
                json.addProperty("journalTitle", journalTitle);
            }

            JsonArray jpArr = new JsonArray();
            for (JournalPageData jp : journalPages) {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", jp.title());
                obj.addProperty("content", jp.content());
                obj.addProperty("timestamp", jp.timestamp());
                jpArr.add(obj);
            }
            json.add("journalPages", jpArr);

            try (Writer w = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
                GSON.toJson(json, w);
            }

            dirty = false;
        } catch (Exception e) {
            Constants.LOG.error("Failed to save field guide progress for {}", playerUUID, e);
        }
    }

    public record JournalPageData(String title, String content, long timestamp) {
    }
}
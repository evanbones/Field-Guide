package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.util.ByteBufCollections;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProgressUpdatePacket implements CustomPacketPayload {
    public static final Type<ProgressUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "progress_update"));
    public static final StreamCodec<FriendlyByteBuf, ProgressUpdatePacket> CODEC = StreamCodec.ofMember(ProgressUpdatePacket::encode, ProgressUpdatePacket::new);

    private final boolean reset;
    private final boolean silent;

    private final List<String> unlocked;
    private final List<String> seen;
    private final Map<String, Long> discoveryTimes;
    private final Map<String, Long> discoveryGameTimes;
    private final List<String> revoked;
    private final Map<String, String> customNames;
    private final Map<String, String> customDescriptions;
    private final Map<String, String> entryPhotographs;
    private final Map<String, String> selectedVariants;
    private final List<String> killedOnly;
    private final List<String> eatenOnly;
    private final Map<String, List<String>> entryTriggers;
    private final Optional<String> journalTitle;
    private final Optional<List<PlayerFieldGuideProgress.JournalPageData>> journalPages;

    private ProgressUpdatePacket(Builder builder) {
        this.reset = builder.reset;
        this.silent = builder.silent;
        this.unlocked = builder.unlocked;
        this.seen = builder.seen;
        this.discoveryTimes = builder.discoveryTimes;
        this.discoveryGameTimes = builder.discoveryGameTimes;
        this.revoked = builder.revoked;
        this.customNames = builder.customNames;
        this.customDescriptions = builder.customDescriptions;
        this.entryPhotographs = builder.entryPhotographs;
        this.selectedVariants = builder.selectedVariants;
        this.killedOnly = builder.killedOnly;
        this.eatenOnly = builder.eatenOnly;
        this.entryTriggers = builder.entryTriggers;
        this.journalTitle = builder.journalTitle;
        this.journalPages = builder.journalPages;
    }

    public ProgressUpdatePacket(FriendlyByteBuf buf) {
        this.reset = buf.readBoolean();
        this.silent = buf.readBoolean();
        this.unlocked = ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf);
        this.seen = ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf);
        this.discoveryTimes = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readLong());
        this.discoveryGameTimes = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readLong());
        this.revoked = ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf);
        this.customNames = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readUtf());
        this.customDescriptions = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readUtf());
        this.entryPhotographs = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readUtf());
        this.selectedVariants = ByteBufCollections.readMap(buf, b -> b.readUtf(), b -> b.readUtf());
        this.killedOnly = ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf);
        this.eatenOnly = ByteBufCollections.readList(buf, FriendlyByteBuf::readUtf);
        this.entryTriggers = ByteBufCollections.readMap(buf, FriendlyByteBuf::readUtf, b -> ByteBufCollections.readList(b, FriendlyByteBuf::readUtf));
        this.journalTitle = buf.readOptional(FriendlyByteBuf::readUtf);
        this.journalPages = buf.readOptional(b -> ByteBufCollections.readList(b, b2 ->
                new PlayerFieldGuideProgress.JournalPageData(b2.readUtf(), b2.readUtf(), b2.readLong())));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        ClientFieldGuideManager.getInstance().applyServerUpdate(this);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(reset);
        buf.writeBoolean(silent);
        ByteBufCollections.writeCollection(buf, unlocked, FriendlyByteBuf::writeUtf);
        ByteBufCollections.writeCollection(buf, seen, FriendlyByteBuf::writeUtf);
        ByteBufCollections.writeMap(buf, discoveryTimes, (b, k) -> b.writeUtf(k), (b, v) -> b.writeLong(v));
        ByteBufCollections.writeMap(buf, discoveryGameTimes, (b, k) -> b.writeUtf(k), (b, v) -> b.writeLong(v));
        ByteBufCollections.writeCollection(buf, revoked, FriendlyByteBuf::writeUtf);
        ByteBufCollections.writeMap(buf, customNames, (b, k) -> b.writeUtf(k), (b, v) -> b.writeUtf(v));
        ByteBufCollections.writeMap(buf, customDescriptions, (b, k) -> b.writeUtf(k), (b, v) -> b.writeUtf(v));
        ByteBufCollections.writeMap(buf, entryPhotographs, (b, k) -> b.writeUtf(k), (b, v) -> b.writeUtf(v));
        ByteBufCollections.writeMap(buf, selectedVariants, (b, k) -> b.writeUtf(k), (b, v) -> b.writeUtf(v));
        ByteBufCollections.writeCollection(buf, killedOnly, FriendlyByteBuf::writeUtf);
        ByteBufCollections.writeCollection(buf, eatenOnly, FriendlyByteBuf::writeUtf);
        ByteBufCollections.writeMap(buf, entryTriggers, FriendlyByteBuf::writeUtf, (b, list) -> ByteBufCollections.writeCollection(b, list, FriendlyByteBuf::writeUtf));
        buf.writeOptional(journalTitle, FriendlyByteBuf::writeUtf);
        buf.writeOptional(journalPages, (b, pages) ->
                ByteBufCollections.writeCollection(b, pages, (b2, page) -> {
                    b2.writeUtf(page.title());
                    b2.writeUtf(page.content());
                    b2.writeLong(page.timestamp());
                }));
    }

    public boolean isReset() {
        return reset;
    }

    public boolean isSilent() {
        return silent;
    }

    public List<String> getUnlocked() {
        return unlocked;
    }

    public List<String> getSeen() {
        return seen;
    }

    public Map<String, Long> getDiscoveryTimes() {
        return discoveryTimes;
    }

    public Map<String, Long> getDiscoveryGameTimes() {
        return discoveryGameTimes;
    }

    public List<String> getRevoked() {
        return revoked;
    }

    public Map<String, String> getCustomNames() {
        return customNames;
    }

    public Map<String, String> getCustomDescriptions() {
        return customDescriptions;
    }

    public Map<String, String> getEntryPhotographs() {
        return entryPhotographs;
    }

    public Map<String, String> getSelectedVariants() {
        return selectedVariants;
    }

    public List<String> getKilledOnly() {
        return killedOnly;
    }

    public List<String> getEatenOnly() {
        return eatenOnly;
    }

    public Map<String, List<String>> getEntryTriggers() {
        return entryTriggers;
    }

    public Optional<String> getJournalTitle() {
        return journalTitle;
    }

    public Optional<List<PlayerFieldGuideProgress.JournalPageData>> getJournalPages() {
        return journalPages;
    }

    public static class Builder {
        private boolean reset = false;
        private boolean silent = false;
        private List<String> unlocked = Collections.emptyList();
        private List<String> seen = Collections.emptyList();
        private Map<String, Long> discoveryTimes = Collections.emptyMap();
        private Map<String, Long> discoveryGameTimes = Collections.emptyMap();
        private List<String> revoked = Collections.emptyList();
        private Map<String, String> customNames = Collections.emptyMap();
        private Map<String, String> customDescriptions = Collections.emptyMap();
        private Map<String, String> entryPhotographs = Collections.emptyMap();
        private Map<String, String> selectedVariants = Collections.emptyMap();
        private List<String> killedOnly = Collections.emptyList();
        private List<String> eatenOnly = Collections.emptyList();
        private Map<String, List<String>> entryTriggers = Collections.emptyMap();
        private Optional<String> journalTitle = Optional.empty();
        private Optional<List<PlayerFieldGuideProgress.JournalPageData>> journalPages = Optional.empty();

        public Builder reset(boolean reset) {
            this.reset = reset;
            return this;
        }

        public Builder silent(boolean silent) {
            this.silent = silent;
            return this;
        }

        public Builder unlocked(List<String> unlocked) {
            this.unlocked = unlocked;
            return this;
        }

        public Builder seen(List<String> seen) {
            this.seen = seen;
            return this;
        }

        public Builder discoveryTimes(Map<String, Long> times) {
            this.discoveryTimes = times;
            return this;
        }

        public Builder discoveryGameTimes(Map<String, Long> gameTimes) {
            this.discoveryGameTimes = gameTimes;
            return this;
        }

        public Builder revoked(List<String> revoked) {
            this.revoked = revoked;
            return this;
        }

        public Builder customNames(Map<String, String> names) {
            this.customNames = names;
            return this;
        }

        public Builder customDescriptions(Map<String, String> desc) {
            this.customDescriptions = desc;
            return this;
        }

        public Builder entryPhotographs(Map<String, String> photos) {
            this.entryPhotographs = photos;
            return this;
        }

        public Builder selectedVariants(Map<String, String> selectedVariants) {
            this.selectedVariants = selectedVariants;
            return this;
        }

        public Builder killedOnly(List<String> killedOnly) {
            this.killedOnly = killedOnly;
            return this;
        }

        public Builder eatenOnly(List<String> eatenOnly) {
            this.eatenOnly = eatenOnly;
            return this;
        }

        public Builder entryTriggers(Map<String, List<String>> triggers) {
            this.entryTriggers = triggers;
            return this;
        }

        public Builder journalTitle(String title) {
            this.journalTitle = Optional.ofNullable(title);
            return this;
        }

        public Builder journalPages(List<PlayerFieldGuideProgress.JournalPageData> pages) {
            this.journalPages = Optional.ofNullable(pages);
            return this;
        }

        public ProgressUpdatePacket build() {
            return new ProgressUpdatePacket(this);
        }
    }
}

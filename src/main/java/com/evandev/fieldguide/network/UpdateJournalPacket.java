package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.FieldGuideLimits;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.util.ByteBufCollections;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateJournalPacket implements CustomPacketPayload {

    public static final Type<UpdateJournalPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_journal"));
    private static final int MAX_TITLE_LENGTH = FieldGuideLimits.MAX_JOURNAL_TITLE_LENGTH;
    private static final int MAX_PAGE_CONTENT_LENGTH = FieldGuideLimits.MAX_JOURNAL_PAGE_CONTENT_LENGTH;
    private static final int MAX_JOURNAL_PAGES = FieldGuideLimits.MAX_JOURNAL_PAGES;
    public static final StreamCodec<FriendlyByteBuf, UpdateJournalPacket> CODEC = StreamCodec.ofMember(
            UpdateJournalPacket::encode,
            UpdateJournalPacket::new
    );
    private final String title;
    private final List<PlayerFieldGuideProgress.JournalPageData> pages;

    public UpdateJournalPacket(String title, List<PlayerFieldGuideProgress.JournalPageData> pages) {
        this.title = title;
        this.pages = pages;
    }

    public UpdateJournalPacket(FriendlyByteBuf buf) {
        this.title = buf.readUtf(MAX_TITLE_LENGTH);
        this.pages = ByteBufCollections.readList(buf, MAX_JOURNAL_PAGES,
                b -> new PlayerFieldGuideProgress.JournalPageData(
                        b.readUtf(MAX_TITLE_LENGTH),
                        b.readUtf(MAX_PAGE_CONTENT_LENGTH),
                        b.readLong()
                )
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(title, MAX_TITLE_LENGTH);
        ByteBufCollections.writeCollection(buf, pages, (b, page) -> {
            b.writeUtf(page.title(), MAX_TITLE_LENGTH);
            b.writeUtf(page.content(), MAX_PAGE_CONTENT_LENGTH);
            b.writeLong(page.timestamp());
        });
    }

    public void handleServer(ServerPlayer player) {
        if (player == null) return;
        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
        if (progress == null) return;

        progress.setJournalTitle(title);
        progress.setJournalPages(pages);
    }
}

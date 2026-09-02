package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.FieldGuideLimits;
import com.evandev.fieldguide.compat.exposure.ExposureCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UpdateEntryDataPacket implements CustomPacketPayload {

    public static final Type<UpdateEntryDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_entry_data"));
    private static final int MAX_NAME_LENGTH = FieldGuideLimits.MAX_ENTRY_NAME_LENGTH;
    private static final int MAX_DESCRIPTION_LENGTH = FieldGuideLimits.MAX_ENTRY_DESCRIPTION_LENGTH;
    public static final StreamCodec<FriendlyByteBuf, UpdateEntryDataPacket> CODEC = StreamCodec.ofMember(
            UpdateEntryDataPacket::encode,
            UpdateEntryDataPacket::new
    );

    private final Action action;
    private final ResourceLocation entryId;
    private final String variantId;
    private final Data data;

    private UpdateEntryDataPacket(Action action, ResourceLocation entryId, String variantId, Data data) {
        this.action = action;
        this.entryId = entryId;
        this.variantId = variantId;
        this.data = data;
    }

    public UpdateEntryDataPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.entryId = buf.readResourceLocation();
        this.variantId = buf.readUtf(128);
        this.data = switch (action) {
            case SET_NAME -> new NameData(buf.readUtf(MAX_NAME_LENGTH));
            case SET_DESCRIPTION -> new DescriptionData(buf.readUtf(MAX_DESCRIPTION_LENGTH));
            case SET_PHOTOGRAPH -> new PhotographData(buf.readVarInt());
            case REMOVE_PHOTOGRAPH -> new RemovePhotographData();
            case SET_SELECTED_VARIANT -> new VariantData(buf.readUtf(128));
        };
    }

    public static UpdateEntryDataPacket setName(ResourceLocation entryId, String name) {
        return new UpdateEntryDataPacket(Action.SET_NAME, entryId, "", new NameData(name));
    }

    public static UpdateEntryDataPacket setVariantName(ResourceLocation entryId, String variantId, String name) {
        return new UpdateEntryDataPacket(Action.SET_NAME, entryId, variantId != null ? variantId : "", new NameData(name));
    }

    public static UpdateEntryDataPacket setDescription(ResourceLocation entryId, String variantId, String description) {
        return new UpdateEntryDataPacket(Action.SET_DESCRIPTION, entryId, variantId != null ? variantId : "", new DescriptionData(description));
    }

    public static UpdateEntryDataPacket setPhotograph(ResourceLocation entryId, int slot, String variantId) {
        return new UpdateEntryDataPacket(Action.SET_PHOTOGRAPH, entryId, variantId != null ? variantId : "", new PhotographData(slot));
    }

    public static UpdateEntryDataPacket removePhotograph(ResourceLocation entryId, String variantId) {
        return new UpdateEntryDataPacket(Action.REMOVE_PHOTOGRAPH, entryId, variantId != null ? variantId : "", new RemovePhotographData());
    }

    public static UpdateEntryDataPacket setSelectedVariant(ResourceLocation entryId, String variantId) {
        return new UpdateEntryDataPacket(Action.SET_SELECTED_VARIANT, entryId, "", new VariantData(variantId));
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeResourceLocation(entryId);
        buf.writeUtf(variantId != null ? variantId : "", 128);
        data.encode(buf);
    }

    public void handleServer(ServerPlayer player) {
        if (player == null) return;
        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
        if (progress == null) return;

        String id = entryId.toString();
        boolean hasVariant = variantId != null && !variantId.isEmpty();
        if (hasVariant) {
            id += "#" + variantId;
        }

        if (hasVariant) {
            boolean allowed = progress.isUnlocked(entryId, variantId)
                    || (ServerConfig.get().unlockAllVariants && progress.isUnlocked(entryId));
            if (!allowed) return;
        } else if (!progress.isUnlocked(entryId)) {
            return;
        }

        data.apply(id, progress, player);
    }

    private enum Action {
        SET_NAME,
        SET_DESCRIPTION,
        SET_PHOTOGRAPH,
        REMOVE_PHOTOGRAPH,
        SET_SELECTED_VARIANT
    }

    private interface Data {
        void encode(FriendlyByteBuf buf);

        void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player);
    }

    private record NameData(String name) implements Data {
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(name != null ? name : "", MAX_NAME_LENGTH);
        }

        @Override
        public void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player) {
            progress.setCustomName(entryId, name);
        }
    }

    private record DescriptionData(String description) implements Data {
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(description != null ? description : "", MAX_DESCRIPTION_LENGTH);
        }

        @Override
        public void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player) {
            progress.setCustomDescription(entryId, description);
        }
    }

    private record PhotographData(int slot) implements Data {
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(slot);
        }

        @Override
        public void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player) {
            if (!Services.PLATFORM.isModLoaded("exposure")) return;

            ItemStack stack = ItemStack.EMPTY;

            if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                stack = player.getInventory().getItem(slot);
            }

            if ((stack.isEmpty() || !ExposureCompat.isPhotographItem(stack)) && slot >= 0 && slot < player.containerMenu.slots.size()) {
                stack = player.containerMenu.getSlot(slot).getItem();
            }

            if (stack.isEmpty() || !ExposureCompat.isPhotographItem(stack)) {
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty() && ExposureCompat.isPhotographItem(carried)) {
                    stack = carried;
                }
            }

            if (stack.isEmpty() || !ExposureCompat.isPhotographItem(stack)) {
                progress.markEntryForResync(entryId);
                return;
            }

            Tag tag = stack.saveOptional(player.registryAccess());
            progress.setEntryPhotograph(entryId, tag.toString());
        }
    }

    private record RemovePhotographData() implements Data {
        @Override
        public void encode(FriendlyByteBuf buf) {
        }

        @Override
        public void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player) {
            progress.setEntryPhotograph(entryId, null);
        }
    }

    private record VariantData(String variantId) implements Data {
        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(variantId != null ? variantId : "", 128);
        }

        @Override
        public void apply(String entryId, PlayerFieldGuideProgress progress, ServerPlayer player) {
            progress.setSelectedVariant(entryId, variantId);
        }
    }
}
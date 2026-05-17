package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.entry.EntryResolutionHelper;
import com.evandev.fieldguide.item.ModItems;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CopyPagePacket(ResourceLocation entryId) implements CustomPacketPayload {

    public static final Type<CopyPagePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "copy_page"));
    public static final StreamCodec<FriendlyByteBuf, CopyPagePacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CopyPagePacket::entryId,
            CopyPagePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleServer(ServerPlayer player) {
        if (player == null) return;
        if (ModItems.PAGE == null) return;
        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
        if (progress == null) return;

        String idStr = entryId.toString();
        if (!progress.isUnlocked(idStr)) return;

        // Check for paper
        boolean hasPaper = player.isCreative() || player.getInventory().contains(new ItemStack(Items.PAPER));
        if (!hasPaper) return;

        // Give item
        ItemStack page = new ItemStack(ModItems.PAGE.get());
        page.set(ModDataComponents.ENTRY_ID.get(), entryId);
        page.set(ModDataComponents.AUTHOR.get(), player.getScoreboardName());

        Component nameComponent = null;
        Optional<Object> entry = EntryResolutionHelper.resolveSingleEntry(entryId, null, null);
        if (entry.isPresent()) {
            Object obj = entry.get();
            switch (obj) {
                case EntityType<?> type -> nameComponent = type.getDescription();
                case Block block -> nameComponent = block.getName();
                case Item item -> nameComponent = item.getDescription();
                default -> {
                }
            }
        }

        if (nameComponent == null) {
            if (entryId.getNamespace().equals("fieldguide") && entryId.getPath().startsWith("cobblemon/")) {
                String species = entryId.getPath().substring("cobblemon/".length());
                int underscore = species.lastIndexOf('_');
                if (underscore != -1) species = species.substring(0, underscore);
                nameComponent = Component.translatable("cobblemon.species." + species + ".name");
            } else {
                nameComponent = Component.translatable(getTranslationKey(entryId));
            }
        }

        page.set(ModDataComponents.ENTRY_NAME.get(), nameComponent);

        List<String> variants = progress.getUnlockedVariants(idStr);
        if (!variants.isEmpty()) {
            page.set(ModDataComponents.VARIANTS.get(), variants);

            Map<String, String> variantNames = new HashMap<>();
            for (String v : variants) {
                String vName = progress.getCustomName(idStr + "#" + v);
                if (vName != null) {
                    variantNames.put(v, vName);
                }
            }

            if (!variantNames.isEmpty()) {
                page.set(ModDataComponents.CUSTOM_VARIANT_NAMES.get(), variantNames);
            }
        }

        String customName = progress.getCustomName(idStr);
        if (customName != null) page.set(ModDataComponents.CUSTOM_NAME.get(), customName);

        String customDescription = progress.getCustomDescription(idStr);
        if (customDescription != null) page.set(ModDataComponents.CUSTOM_DESCRIPTION.get(), customDescription);

        String photograph = progress.getEntryPhotograph(idStr);
        if (photograph != null) page.set(ModDataComponents.PHOTOGRAPH.get(), photograph);

        page.set(ModDataComponents.DISCOVERY_TIME.get(), progress.getDiscoveryTime(idStr));
        page.set(ModDataComponents.DISCOVERY_GAME_TIME.get(), progress.getDiscoveryGameTime(idStr));

        if (!player.getInventory().add(page)) {
            player.drop(page, false);
        }

        // Remove paper
        if (!player.isCreative()) {
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.PAPER), 1, player.inventoryMenu.getCraftSlots());
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private String getTranslationKey(ResourceLocation id) {
        return "fieldguide.entry." + id.getNamespace() + "." + id.getPath().replace("/", ".");
    }
}
package com.evandev.fieldguide.network;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
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

//? if >=26.3 {
/*import net.minecraft.util.Prediction;
*///?}

public record CopyPagePacket(ResourceLocation entryId) implements CustomPacketPayload {

    public static final Type<CopyPagePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "copy_page"));
    public static final StreamCodec<FriendlyByteBuf, CopyPagePacket> CODEC = StreamCodec.ofMember(CopyPagePacket::encode, CopyPagePacket::new);

    public CopyPagePacket(FriendlyByteBuf buf) {
        this(buf.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(entryId);
    }

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
        ModDataComponents.set(page, ModDataComponents.ENTRY_ID, entryId);
        ModDataComponents.set(page, ModDataComponents.AUTHOR, player.getScoreboardName());

        Component nameComponent = null;
        Optional<Object> entry = EntryResolutionHelper.resolveSingleEntry(entryId, null, null);
        if (entry.isPresent()) {
            Object obj = entry.get();
            if (obj instanceof EntityType<?> type) nameComponent = type.getDescription();
            else if (obj instanceof Block block) nameComponent = block.getName();
            //? if <26.1 {
            else if (obj instanceof Item item) nameComponent = item.getDescription();
            //?} else {
            /*else if (obj instanceof Item item) nameComponent = Component.translatable(item.getDescriptionId());
            *///?}
        }

        if (nameComponent == null) {
            if (FieldGuideCobblemonCompat.isCobblemonId(entryId)) {
                nameComponent = Component.translatable(FieldGuideCobblemonCompat.getSpeciesTranslationKey(entryId));
            } else {
                nameComponent = Component.translatable(getTranslationKey(entryId));
            }
        }

        ModDataComponents.set(page, ModDataComponents.ENTRY_NAME, nameComponent);

        List<String> variants = progress.getUnlockedVariants(idStr);
        if (!variants.isEmpty()) {
            ModDataComponents.set(page, ModDataComponents.VARIANTS, variants);

            Map<String, String> variantNames = new HashMap<>();
            for (String v : variants) {
                String vName = progress.getCustomName(idStr + "#" + v);
                if (vName != null) {
                    variantNames.put(v, vName);
                }
            }

            if (!variantNames.isEmpty()) {
                ModDataComponents.set(page, ModDataComponents.CUSTOM_VARIANT_NAMES, variantNames);
            }
        }

        String customName = progress.getCustomName(idStr);
        if (customName != null) ModDataComponents.set(page, ModDataComponents.CUSTOM_NAME, customName);

        String customDescription = progress.getCustomDescription(idStr);
        if (customDescription != null) ModDataComponents.set(page, ModDataComponents.CUSTOM_DESCRIPTION, customDescription);

        String photograph = progress.getEntryPhotograph(idStr);
        if (photograph != null) ModDataComponents.set(page, ModDataComponents.PHOTOGRAPH, photograph);

        ModDataComponents.set(page, ModDataComponents.DISCOVERY_TIME, progress.getDiscoveryTime(idStr));
        ModDataComponents.set(page, ModDataComponents.DISCOVERY_GAME_TIME, progress.getDiscoveryGameTime(idStr));

        if (!player.getInventory().add(page)) {
            //? if <26.3 {
            player.drop(page, false);
            //?} else {
            /*player.drop(page, false, Prediction.SERVER_ONLY);
            *///?}
        }

        // Remove paper
        if (!player.isCreative()) {
            //? if <26.3 {
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.PAPER), 1, player.inventoryMenu.getCraftSlots());
            //?} else {
            /*player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.PAPER), false, 1, player.inventoryMenu.getCraftSlots());
            *///?}
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private String getTranslationKey(ResourceLocation id) {
        return "fieldguide.entry." + id.getNamespace() + "." + id.getPath().replace("/", ".");
    }
}

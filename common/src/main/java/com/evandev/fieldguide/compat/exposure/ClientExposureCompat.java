package com.evandev.fieldguide.compat.exposure;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuidePhotographScreen;
import com.evandev.fieldguide.client.gui.widget.FieldGuidePhotographWidget;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.gui.screen.ItemListScreen;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClientExposureCompat {
    private static final WidgetSprites ADD_PHOTO_SPRITES = new WidgetSprites(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/exposure/add_photo"), ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/exposure/add_photo_highlighted"));
    private static final ResourceLocation MISSING_PHOTOGRAPH_BACKGROUND = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/exposure/missing_photograph.png");

    public static boolean willRenderAddPhotoButton(Object entry, String variantId) {
        if (!ClientFieldGuideManager.isUnlocked(entry)) return false;
        if (variantId != null && !variantId.isEmpty() && !ServerConfig.get().unlockAllVariants
                && !ClientFieldGuideManager.isVariantUnlocked(entry, variantId)) return false;
        if (!ClientConfig.get().exposureAddPhotographButton) return false;
        return ProgressManager.getInstance().getPhotograph(entry, variantId).isEmpty();
    }

    public static void setupExposureWidgets(FieldGuideEntryScreen screen, Object entry, String variantId) {
        if (!ClientFieldGuideManager.isUnlocked(entry)) return;

        if (variantId != null && !variantId.isEmpty() && !ServerConfig.get().unlockAllVariants) {
            if (!ClientFieldGuideManager.isVariantUnlocked(entry, variantId)) return;
        }

        int leftX = screen.getLeftPageBounds().left();
        int leftY = screen.getLeftPageBounds().top();
        int leftWidth = screen.getLeftPageBounds().width();
        int leftHeight = screen.getLeftPageBounds().height();

        int iconSize = 16;
        int iconX = leftX + leftWidth - iconSize - 12;
        int iconY = leftY + 12;

        ItemStack existingPhoto = ProgressManager.getInstance().getPhotograph(entry, variantId);

        if (existingPhoto.isEmpty()) {
            if (ClientConfig.get().exposureAddPhotographButton) {
                ImageButton addPhotoButton = new ImageButton(iconX, iconY, iconSize, iconSize, ADD_PHOTO_SPRITES, btn -> {
                    openPhotographSelector(screen, entry, variantId);
                }, Component.translatable("gui.fieldguide.add_photograph"));

                addPhotoButton.setTooltip(Tooltip.create(Component.translatable("gui.fieldguide.add_photograph")));
                screen.addExposureWidget(addPhotoButton);
            }
        } else {
            int photoWidth = 108;
            int photoHeight = 108;
            int photoX = leftX + (leftWidth / 2) - (photoWidth / 2);
            int photoY = leftY + (leftHeight / 2) - (photoHeight / 2) - 15;

            Rect2i exposureArea = new Rect2i(photoX + 6, photoY + 6, photoWidth - 12, photoHeight - 12);

            Component tooltipText = Component.empty()
                    .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Left Click").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("] or [").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Scroll Up").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("] to View\n").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Right Click").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("] to Remove").withStyle(ChatFormatting.DARK_GRAY));

            FieldGuidePhotographWidget photoWidget = new FieldGuidePhotographWidget(
                    photoX, photoY, photoWidth, photoHeight,
                    exposureArea,
                    () -> ProgressManager.getInstance().getPhotograph(entry, variantId),

                    () -> Minecraft.getInstance().setScreen(new FieldGuidePhotographScreen(screen, List.of(new ItemAndStack<>(existingPhoto)))),

                    () -> {
                        ProgressManager.getInstance().setPhotograph(entry, -1, ItemStack.EMPTY, variantId);
                        FieldGuideEntryScreen newScreen = new FieldGuideEntryScreen(screen.getParentScreen(), entry);
                        newScreen.setInitialVariant(variantId);
                        Minecraft.getInstance().setScreen(newScreen);
                    },
                    tooltipText
            );

            screen.addExposureWidget(photoWidget);
        }
    }

    public static void renderMissingPhotoBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.blit(MISSING_PHOTOGRAPH_BACKGROUND, x, y, 0, 0, width, height, width, height);
    }

    public static void renderPhotographInGrid(GuiGraphics guiGraphics, int x, int y, int width, int height, ItemStack photograph) {
        if (photograph.getItem() instanceof PhotographItem) {
            PhotographStyle style = PhotographStyle.of(photograph);
            Rect2i exposureArea = new Rect2i(x + 4, y + 4, width - 8, height - 8);
            int textureSize = 64;
            int sliceSize = width / 2;

            guiGraphics.blit(style.paperTexture(), x, y, 0, 0, 0, sliceSize, sliceSize, textureSize, textureSize);
            guiGraphics.blit(style.paperTexture(), x + sliceSize, y, 0, textureSize - sliceSize, 0, sliceSize, sliceSize, textureSize, textureSize);
            guiGraphics.blit(style.paperTexture(), x, y + sliceSize, 0, 0, textureSize - sliceSize, sliceSize, sliceSize, textureSize, textureSize);
            guiGraphics.blit(style.paperTexture(), x + sliceSize, y + sliceSize, 0, textureSize - sliceSize, textureSize - sliceSize, sliceSize, sliceSize, textureSize, textureSize);
            guiGraphics.flush();

            guiGraphics.pose().pushPose();
            float scale = (float) exposureArea.getWidth();
            guiGraphics.pose().translate(exposureArea.getX(), exposureArea.getY(), 1);
            guiGraphics.pose().scale(scale, scale, scale);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            ExposureClient.photographRenderer().render(photograph, false, false, guiGraphics.pose(),
                    bufferSource, LightTexture.FULL_BRIGHT, 255, 255, 255, 255);
            bufferSource.endBatch();
            guiGraphics.pose().popPose();

            if (style.hasOverlayTexture()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 2);
                guiGraphics.blit(style.overlayTexture(), x, y, 0, 0, 0, sliceSize, sliceSize, textureSize, textureSize);
                guiGraphics.blit(style.overlayTexture(), x + sliceSize, y, 0, textureSize - sliceSize, 0, sliceSize, sliceSize, textureSize, textureSize);
                guiGraphics.blit(style.overlayTexture(), x, y + sliceSize, 0, 0, textureSize - sliceSize, sliceSize, sliceSize, textureSize, textureSize);
                guiGraphics.blit(style.overlayTexture(), x + sliceSize, y + sliceSize, 0, textureSize - sliceSize, textureSize - sliceSize, sliceSize, sliceSize, textureSize, textureSize);
                guiGraphics.pose().popPose();
            }
        }
    }

    private static void openPhotographSelector(FieldGuideEntryScreen parent, Object entry, String variantId) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> photographs = new ArrayList<>();
        Map<ItemStack, Integer> slotMap = new IdentityHashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PhotographItem) {
                photographs.add(stack);
                slotMap.put(stack, i);
            }
        }

        Minecraft.getInstance().setScreen(new PhotographSelectionScreen(parent, photographs, stack -> {
            Integer slot = slotMap.get(stack);
            if (slot != null) {
                ProgressManager.getInstance().setPhotograph(entry, slot, stack, variantId);
            }
            FieldGuideEntryScreen newScreen = new FieldGuideEntryScreen(parent.getParentScreen(), entry);
            newScreen.setInitialVariant(variantId);
            Minecraft.getInstance().setScreen(newScreen);
        }));
    }

    public static boolean hasPhotograph(Object entry, String variantId) {
        return !ProgressManager.getInstance().getPhotograph(entry, variantId).isEmpty();
    }

    private static class PhotographSelectionScreen extends ItemListScreen {
        private final Consumer<ItemStack> onSelect;

        public PhotographSelectionScreen(Screen parent, List<ItemStack> items, Consumer<ItemStack> onSelect) {
            super(parent, Component.translatable("gui.fieldguide.select_photograph"), items);
            this.onSelect = onSelect;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hoveredSlot != null && hoveredSlot.hasItem()) {
                onSelect.accept(hoveredSlot.getItem());
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}

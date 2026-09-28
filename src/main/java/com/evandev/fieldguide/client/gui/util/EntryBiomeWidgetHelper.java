package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.client.gui.widget.PaginatedGridWidget;
import com.evandev.fieldguide.client.gui.widget.VariantOverviewWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

//? if >=26.1 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

public final class EntryBiomeWidgetHelper {

    private EntryBiomeWidgetHelper() {
    }

    public static PaginatedGridWidget<ResourceLocation> createBiomeGridWidget(
            Bounds rightPageBounds,
            List<ResourceLocation> spawnBiomes,
            Font font,
            Supplier<VariantOverviewWidget> variantOverviewSupplier,
            Screen parentScreen
    ) {
        int itemSize = 20;
        int x = rightPageBounds.left() + 2;
        int y = rightPageBounds.bottom() - 33;
        int width = rightPageBounds.width() - 4;

        return new PaginatedGridWidget<>(x, y, width, itemSize, 5, itemSize, 0, new ArrayList<>(spawnBiomes), (graphics, item, itemX, itemY, mouseX, mouseY) -> {
            var resourceManager = Minecraft.getInstance().getResourceManager();

            ResourceLocation baseTexture = ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "textures/immersiveoverlays/" + item.getPath() + ".png");
            ResourceLocation txtFile = ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "textures/immersiveoverlays/" + item.getPath() + ".txt");

            ResourceLocation renderTexture = baseTexture;

            if (resourceManager.getResource(txtFile).isPresent()) {
                try (BufferedReader reader = resourceManager.getResource(txtFile).get().openAsReader()) {
                    String redirectStr = reader.readLine();
                    if (redirectStr != null && !redirectStr.trim().isEmpty()) {
                        ResourceLocation redirectLoc = ResourceLocation.parse(redirectStr.trim());
                        renderTexture = ResourceLocation.fromNamespaceAndPath(redirectLoc.getNamespace(), "textures/immersiveoverlays/" + redirectLoc.getPath() + ".png");
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to read Immersive Overlay redirect file for biome {}", item, e);
                }
            }

            VariantOverviewWidget variantOverview = variantOverviewSupplier.get();
            boolean mouseOver = Bounds.isMouseOver(mouseX, mouseY, itemX, itemY, itemSize, itemSize) && (variantOverview == null || !variantOverview.isMouseOver(mouseX, mouseY));
            int backgroundOffset = mouseOver ? itemSize : 0;
            GuiCompat.blit(graphics, Constants.WIDGETS_TEXTURE, itemX, itemY, 20, 64 + backgroundOffset, itemSize, itemSize, 256, 256);
            int offset = (itemSize - 16) / 2;

            if (resourceManager.getResource(renderTexture).isPresent()) {
                GuiCompat.blit(graphics, renderTexture, itemX + offset, itemY + offset, 0, 0, 16, 16, 16, 16);
            } else if (resourceManager.getResource(baseTexture).isPresent()) {
                GuiCompat.blit(graphics, baseTexture, itemX + offset, itemY + offset, 0, 0, 16, 16, 16, 16);
            } else {
                ResourceLocation plainsTexture = ResourceLocation.withDefaultNamespace("textures/immersiveoverlays/plains.png");
                if (resourceManager.getResource(plainsTexture).isPresent()) {
                    GuiCompat.blit(graphics, plainsTexture, itemX + offset, itemY + offset, 0, 0, 16, 16, 16, 16);
                }
            }

            if (Bounds.isMouseOver(mouseX, mouseY, itemX + offset, itemY + offset, 16, 16)) {
                graphics.renderTooltip(font, Component.translatable("biome." + item.getNamespace() + "." + item.getPath()), mouseX, mouseY);
            }
        }, item -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new FieldGuideCategoryScreen("=!" + item, parentScreen));
        });
    }
}

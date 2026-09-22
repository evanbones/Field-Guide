package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.attribute.GuideAttribute;
import com.evandev.fieldguide.config.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class EntryAttributesRenderer {

    private EntryAttributesRenderer() {
    }

    public static void renderAttributes(
            GuiGraphics guiGraphics,
            Font font,
            List<GuideAttribute> activeAttributes,
            int currentY,
            int startX,
            int mouseX,
            int mouseY,
            Consumer<Component> tooltipSetter
    ) {
        if (activeAttributes == null || activeAttributes.isEmpty()) return;

        RenderSystem.setShaderColor(1, 1, 1, 1);

        int iconSize = 9;
        int iconSpacing = 2;
        int gap = 1;
        int horizontalPadding = 4;
        int barHeight = 15;

        int drawX = startX;
        int iconYOffset = 3;
        int textYOffset = 4;

        for (GuideAttribute attr : activeAttributes) {
            RenderSystem.setShaderTexture(0, attr.icon());
            guiGraphics.blit(attr.icon(), drawX + horizontalPadding, currentY + iconYOffset + (iconSize - attr.height()) / 2, attr.u(), attr.v(), attr.width(), attr.height(), attr.textureWidth(), attr.textureHeight());
            int textWidth = 0;
            if (attr.value() != null) {
                textWidth = font.width(attr.value());
                guiGraphics.drawString(font, attr.value(), drawX + horizontalPadding + attr.width() + iconSpacing, currentY + textYOffset, ClientConfig.get().getTextColorInt(), false);
                textWidth += iconSpacing;
            }
            Bounds attrBounds = new Bounds(drawX, currentY, attr.width() + horizontalPadding * 2 + textWidth, barHeight);

            drawX += attrBounds.width();
            guiGraphics.blit(Constants.ATTRIBUTES_SEPARATOR, drawX, currentY, 0, 0, 1, 15, 1, 15);
            drawX += gap;

            if (attrBounds.contains(mouseX, mouseY) && tooltipSetter != null) {
                tooltipSetter.accept(attr.tooltip());
            }
        }
    }
}

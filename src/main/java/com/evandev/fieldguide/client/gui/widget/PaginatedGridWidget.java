package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

//? if >=26.1 {
/*import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

public class PaginatedGridWidget<T> extends AbstractWidget {
    private final List<T> items;
    private final int itemsPerPage;
    private final int itemSize;
    private final int spacing;
    private final ImageButton prevButton;
    private final ImageButton nextButton;
    private final ItemRenderer<T> itemRenderer;
    private final Consumer<T> onClick;
    private int currentPage = 1;

    public PaginatedGridWidget(int x, int y, int width, int height, int itemsPerPage, int itemSize, int spacing, List<T> items, ItemRenderer<T> itemRenderer, Consumer<T> onClick) {
        super(x, y, width, height, Component.empty());
        this.items = items;
        this.itemsPerPage = itemsPerPage;
        this.itemSize = itemSize;
        this.spacing = spacing;
        this.itemRenderer = itemRenderer;
        this.onClick = onClick;
        int buttonSize = 16;
        int buttonYOffset = (itemSize - buttonSize) / 2;

        this.prevButton = new ImageButton(x, y + buttonYOffset, buttonSize, buttonSize, ClientConstants.PREV_SPRITES, b -> setPage(currentPage - 1));
        this.nextButton = new ImageButton(x + width - 16, y + buttonYOffset, buttonSize, buttonSize, ClientConstants.NEXT_SPRITES, b -> setPage(currentPage + 1));
        updateButtons();
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    private void setPage(int page) {
        this.currentPage = Math.max(1, Math.min(getTotalPages(), page));
        updateButtons();
    }

    private void updateButtons() {
        this.prevButton.visible = getTotalPages() > 1;
        this.nextButton.visible = getTotalPages() > 1;
        this.prevButton.active = currentPage > 1;
        this.nextButton.active = currentPage < getTotalPages();
    }

    @Override
    //? if <26.1 {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (nextButton.mouseClicked(mouseX, mouseY, button)) return true;
    //?} else {
    /*public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (prevButton.mouseClicked(event, doubleClick)) return true;
        if (nextButton.mouseClicked(event, doubleClick)) return true;
    *///?}

        if (button == 0 && onClick != null && !items.isEmpty()) {
            int indexStart = itemsPerPage * (currentPage - 1);
            int indexEnd = Math.min(items.size(), itemsPerPage * currentPage);
            int itemsToDraw = indexEnd - indexStart;

            int totalWidth = itemsToDraw * itemSize + Math.max(0, itemsToDraw - 1) * spacing;
            int currentX = this.getX() + (this.width / 2) - (totalWidth / 2);

            for (int i = indexStart; i < indexEnd; i++) {
                if (mouseX >= currentX && mouseX <= currentX + itemSize && mouseY >= this.getY() && mouseY <= this.getY() + itemSize) {
                    onClick.accept(items.get(i));
                    return true;
                }
                currentX += itemSize + spacing;
            }
        }

        //? if <26.1 {
        return super.mouseClicked(mouseX, mouseY, button);
        //?} else {
        /*return super.mouseClicked(event, doubleClick);
        *///?}
    }

    @Override
    //? if >=1.21 {
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    //?} else {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        double scrollX = 0;
    *///?}
        if (this.isHoveredOrFocused()) {
            setPage(currentPage - (int) Math.signum(scrollY));
            return true;
        }
        return false;
    }

    @Override
    //? if <26.1 {
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void extractWidgetRenderState(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    *///?}
        if (items.isEmpty()) return;

        int indexStart = itemsPerPage * (currentPage - 1);
        int indexEnd = Math.min(items.size(), itemsPerPage * currentPage);
        int itemsToDraw = indexEnd - indexStart;

        int totalWidth = itemsToDraw * itemSize + Math.max(0, itemsToDraw - 1) * spacing;
        int currentX = this.getX() + (this.width / 2) - (totalWidth / 2);

        for (int i = indexStart; i < indexEnd; i++) {
            itemRenderer.render(guiGraphics, items.get(i), currentX, this.getY(), mouseX, mouseY);
            currentX += itemSize + spacing;
        }

        //? if <26.1 {
        prevButton.render(guiGraphics, mouseX, mouseY, partialTick);
        nextButton.render(guiGraphics, mouseX, mouseY, partialTick);
        //?} else {
        /*prevButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        nextButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        *///?}

        if (getTotalPages() > 1) {
            int barY = this.getY() + itemSize;
            int barWidth = (itemSize + spacing) * itemsPerPage - spacing;
            int barStartX = this.getX() + (width / 2) - (barWidth / 2);

            float progressStart = (float) (currentPage - 1) / getTotalPages();
            float progressEnd = (float) currentPage / getTotalPages();
            int progressWidth = (int) (barWidth * progressEnd) - (int) (barWidth * progressStart);
            int barHeight = 5;

            GuiCompat.blitSprite(guiGraphics, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_bar_bg"), barStartX, barY, barWidth, barHeight);
            GuiCompat.blitSprite(guiGraphics, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "widget/grid_bar_fill"), barStartX + (int) (barWidth * progressStart), barY, progressWidth, barHeight);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    public interface ItemRenderer<T> {
        void render(GuiGraphics graphics, T item, int x, int y, int mouseX, int mouseY);
    }
}

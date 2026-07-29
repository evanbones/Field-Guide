package com.evandev.fieldguide.client.gui.screens;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.FieldGuideLimits;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.data.JournalPage;
import com.evandev.fieldguide.client.gui.widget.FieldGuideSearchBox;
import com.evandev.fieldguide.client.gui.widget.PageTurnButton;
import com.evandev.fieldguide.compat.scholar.ScholarCompat;
import com.evandev.fieldguide.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FieldGuideJournalScreen extends BookScreen {
    public static int lastOpenedJournalPage = 0;
    private int currentSpread;
    private FieldGuideSearchBox searchBox;

    public FieldGuideJournalScreen(Category category, int pageIndex) {
        super(Component.literal("Journal"));
        this.setSelectedCategory(category);
        this.currentSpread = pageIndex;
        lastOpenedJournalPage = pageIndex;
    }

    private String getDefaultJournalText() {
        return I18n.get("fieldguide.journal.default");
    }

    @Override
    protected void init() {
        super.init();
        ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
        List<JournalPage> pages = manager.getJournalPages();
        ensurePagesExist(pages);
        clampCurrentSpread(pages);

        int textXLeft = this.leftPageBounds.left() + 6;
        int textXRight = this.rightPageBounds.left() + 6;
        int titleY = this.leftPageBounds.top() + 8;
        int textY = titleY + this.font.lineHeight * 2 + 13;
        int textAreaWidth = this.rightPageBounds.width() - 12;
        int textAreaHeight = this.leftPageBounds.height() - (textY - this.leftPageBounds.top()) - 11;

        // Journal Title
        if (currentSpread == 0) {
            String jTitle = manager.getJournalTitle();
            int jtWidth = Math.max(100, this.font.width(jTitle.isEmpty() ? "Journal" : jTitle));
            AbstractWidget journalTitleWidget = ScholarCompat.createTextField(this.font, this.leftPageBounds.x_center() - jtWidth / 2, this.leftPageBounds.top() + 36, jtWidth, font.lineHeight, jTitle, ClientConfig.get().getTextTitleColorInt(), 100, FieldGuideLimits.MAX_JOURNAL_TITLE_LENGTH, manager::setJournalTitle, true);
            this.addRenderableWidget(journalTitleWidget);
        } else {
            JournalPage lPage = pages.get(currentSpread * 2 - 1);

            AbstractWidget leftTitleWidget = ScholarCompat.createTextField(this.font, textXLeft, titleY, textAreaWidth, font.lineHeight, lPage.title, ClientConfig.get().getTextTitleColorInt(), textAreaWidth, FieldGuideLimits.MAX_JOURNAL_PAGE_TITLE_LENGTH, text -> {
                lPage.title = text;
                manager.saveJournal();
            }, false);

            AbstractWidget leftContentWidget = ScholarCompat.createTextArea(this.font, textXLeft, textY, textAreaWidth, textAreaHeight, 12, 9, ClientConfig.get().getTextColorInt(), false, FieldGuideLimits.MAX_JOURNAL_PAGE_CONTENT_LENGTH, lPage.content, text -> {
                lPage.content = text;
                manager.saveJournal();
            }, spill -> handleSpillover(spill, currentSpread * 2));
            this.addRenderableWidget(leftTitleWidget);
            this.addRenderableWidget(leftContentWidget);
        }

        // Right Page
        JournalPage rPage = pages.get(currentSpread == 0 ? 0 : currentSpread * 2);

        AbstractWidget rightTitleWidget = ScholarCompat.createTextField(this.font, textXRight, titleY, textAreaWidth, font.lineHeight, rPage.title, ClientConfig.get().getTextTitleColorInt(), textAreaWidth, FieldGuideLimits.MAX_JOURNAL_PAGE_TITLE_LENGTH, text -> {
            rPage.title = text;
            manager.saveJournal();
        }, false);

        AbstractWidget rightContentWidget = ScholarCompat.createTextArea(this.font, textXRight, textY, textAreaWidth, textAreaHeight, 12, 9, ClientConfig.get().getTextColorInt(), false, FieldGuideLimits.MAX_JOURNAL_PAGE_CONTENT_LENGTH, rPage.content, text -> {
            rPage.content = text;
            manager.saveJournal();
        }, spill -> handleSpillover(spill, (currentSpread == 0 ? 0 : currentSpread * 2) + 1));

        this.addRenderableWidget(rightTitleWidget);
        this.addRenderableWidget(rightContentWidget);

        // Navigation
        PageTurnButton prevButton = new PageTurnButton(this.bounds.left() + 15, this.leftPageBounds.bottom() - 15, 16, 16, ClientConstants.PREV_PAGE_SPRITES, b -> changeSpread(-1));
        PageTurnButton nextButton = new PageTurnButton(this.bounds.right() - 30, this.rightPageBounds.bottom() - 15, 16, 16, ClientConstants.NEXT_PAGE_SPRITES, b -> changeSpread(1));
        prevButton.visible = currentSpread > 0;
        int nextSpreadPageCount = (currentSpread + 1) * 2 + 1;
        nextButton.visible = nextSpreadPageCount <= FieldGuideLimits.MAX_JOURNAL_PAGES;
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);

        this.searchBox = new FieldGuideSearchBox(this.font, this.width / 2 - 70, this.bounds.bottom() + 5, 140, 20, "", q -> {
            if (!q.isEmpty()) {
                FieldGuideCategoryScreen searchScreen = new FieldGuideCategoryScreen(q, this);
                searchScreen.setInitialSearchFocus(true);
                this.minecraft.gui.setScreen(searchScreen);
            }
        });
        this.addRenderableWidget(this.searchBox);
    }

    private void clampCurrentSpread(List<JournalPage> pages) {
        int maxSpread = Math.max(0, (pages.size() - 1) / 2);
        if (currentSpread > maxSpread) {
            currentSpread = maxSpread;
        }
    }

    private void ensurePagesExist(List<JournalPage> pages) {
        int targetSize = Math.min(currentSpread == 0 ? 1 : currentSpread * 2 + 1, FieldGuideLimits.MAX_JOURNAL_PAGES);
        while (pages.size() < targetSize) {
            pages.add(new JournalPage("", getDefaultJournalText(), System.currentTimeMillis()));
        }
    }

    private void changeSpread(int direction) {
        cleanupEmptyPages();
        currentSpread += direction;
        Minecraft.getInstance().gui.setScreen(new FieldGuideJournalScreen(this.getSelectedCategory(), currentSpread));
    }

    private void handleSpillover(String spill, int targetPageIndex) {
        if (targetPageIndex >= FieldGuideLimits.MAX_JOURNAL_PAGES) return;
        ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
        List<JournalPage> pages = manager.getJournalPages();
        while (pages.size() <= targetPageIndex) {
            pages.add(new JournalPage("", getDefaultJournalText(), System.currentTimeMillis()));
        }
        JournalPage targetPage = pages.get(targetPageIndex);
        targetPage.content = spill + targetPage.content;
        manager.saveJournal();

        Minecraft.getInstance().gui.setScreen(new FieldGuideJournalScreen(this.getSelectedCategory(), currentSpread));
    }

    private void cleanupEmptyPages() {
        ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
        List<JournalPage> pages = manager.getJournalPages();
        for (int i = pages.size() - 1; i > 0; i--) {
            if (pages.get(i).title.trim().isEmpty() && (pages.get(i).content.trim().isEmpty() || pages.get(i).content.equals(getDefaultJournalText())))
                pages.remove(i);
            else break;
        }
        manager.saveJournal();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.@NonNull KeyEvent event) {
        if (this.getFocused() instanceof AbstractWidget widget && widget.isFocused()) {
            if (this.minecraft.options.keyInventory.matches(event)) return true;
            if (FieldGuideClient.OPEN_GUIDE_KEY.matches(event)) return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (this.searchBox != null) this.searchBox.setFocused(this.searchBox.isMouseOver(event.x(), event.y()));
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onTabClick(Category category) {
        if (category.getId().getPath().equals("intro")) return;
        cleanupEmptyPages();
        Objects.requireNonNull(this.minecraft).gui.setScreen(new FieldGuideCategoryScreen(category, 0));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (scrollY > 0 && currentSpread > 0) {
            changeSpread(-1);
            return true;
        } else if (scrollY < 0) {
            int nextSpreadPageCount = (currentSpread + 1) * 2 + 1;
            if (nextSpreadPageCount <= FieldGuideLimits.MAX_JOURNAL_PAGES) {
                changeSpread(1);
                return true;
            }
        }

        return false;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        List<JournalPage> pages = ClientFieldGuideManager.getInstance().getJournalPages();
        clampCurrentSpread(pages);
        this.extractFieldGuideBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().pushMatrix();

        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Constants.BOOK_TEXTURE, this.bounds.left(), this.bounds.top(), 0, 0, this.bounds.width(), this.bounds.height(), this.bounds.width(), this.bounds.height());

        if (currentSpread == 0) {
            guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Constants.JOURNAL_TITLE_PAGE_TEXTURE, this.bounds.left(), this.bounds.top(), 0, 0, this.bounds.width(), this.bounds.height(), this.bounds.width(), this.bounds.height());
            guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Constants.JOURNAL_PAGE_TEXTURE, this.bounds.x_center(), this.bounds.top(), (float) this.bounds.width() / 2, 0, this.bounds.width() / 2, this.bounds.height(), this.bounds.width(), this.bounds.height());
        } else {
            guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Constants.JOURNAL_PAGE_TEXTURE, this.bounds.left(), this.bounds.top(), 0, 0, this.bounds.width(), this.bounds.height(), this.bounds.width(), this.bounds.height());
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // Page Numbers and Dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        int dateY = this.leftPageBounds.top() + 8 + this.font.lineHeight + 2;

        if (currentSpread > 0) {
            String leftPageStr = (currentSpread * 2) + "";
            guiGraphics.text(this.font, leftPageStr, this.leftPageBounds.x_center() - this.font.width(leftPageStr) / 2, this.leftPageBounds.bottom() - 11, ClientConfig.get().getPageNumberColorInt(), false);

            JournalPage lPage = pages.get(currentSpread * 2 - 1);
            guiGraphics.text(this.font, dateFormat.format(new Date(lPage.timestamp)), this.leftPageBounds.left() + 6, dateY, ClientConfig.get().getTextMutedColorInt(), false);
        }

        String rightPageStr = (currentSpread * 2 + 1) + "";
        guiGraphics.text(this.font, rightPageStr, this.rightPageBounds.x_center() - this.font.width(rightPageStr) / 2, this.rightPageBounds.bottom() - 11, ClientConfig.get().getPageNumberColorInt(), false);

        JournalPage rPage = pages.get(currentSpread == 0 ? 0 : currentSpread * 2);
        guiGraphics.text(this.font, dateFormat.format(new Date(rPage.timestamp)), this.rightPageBounds.left() + 6, dateY, ClientConfig.get().getTextMutedColorInt(), false);

        guiGraphics.pose().popMatrix();
    }
}
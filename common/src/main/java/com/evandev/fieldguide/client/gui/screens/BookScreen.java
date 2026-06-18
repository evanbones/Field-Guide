package com.evandev.fieldguide.client.gui.screens;

import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.gui.util.Bounds;
import com.evandev.fieldguide.client.gui.widget.TabButton;
import com.evandev.fieldguide.compat.nomansland.NoMansLandCompat;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.api.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class BookScreen extends Screen {
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 200;
    private static final int PAGE_WIDTH = 134;
    private static final int PAGE_HEIGHT = 164;

    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_GAP = -1;
    private static final int TAB_Y_OFFSET = 32;
    private static final int MAX_TABS = 6;
    public static BookScreen lastOpenedScreen;
    private static int tabStartIndex = 0;
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final List<Category> sortedCategories = new ArrayList<>();
    protected Bounds bounds;
    protected Bounds leftPageBounds;
    protected Bounds rightPageBounds;
    private Button tabUpButton;
    private Button tabDownButton;
    private Category selectedCategory;

    protected BookScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        lastOpenedScreen = this;
        this.bounds = new Bounds((this.width - BG_WIDTH) / 2, (this.height - BG_HEIGHT) / 2, BG_WIDTH, BG_HEIGHT);
        this.leftPageBounds = new Bounds(this.bounds.left() + 16, this.bounds.top() + 19, PAGE_WIDTH, PAGE_HEIGHT);
        this.rightPageBounds = new Bounds(this.leftPageBounds.right() + 1, leftPageBounds.top(), PAGE_WIDTH, PAGE_HEIGHT);

        initCategories();
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null) {
            Minecraft minecraft = this.minecraft;
            minecraft.tell(() -> {
                Screen current = minecraft.screen;
                if (current == null || !current.getClass().getName().startsWith("com.evandev.fieldguide."))
                    NoMansLandCompat.stopReplay();
            });
        }
    }

    @Override
    public boolean isPauseScreen() {
        return !NoMansLandCompat.isAvailable();
    }

    private void initCategories() {
        // Sort categories
        this.sortedCategories.clear();
        for (Category cat : ClientFieldGuideManager.getCategories().values()) {
            List<Object> entries = ClientFieldGuideManager.getInstance().getEntriesForCategory(cat);
            boolean isIntro = cat.getId().getPath().equals("intro");

            boolean hasUnlocked = false;
            if (ServerConfig.get().hideTabsUntilUnlocked && !isIntro && entries != null) {
                for (Object entry : entries) {
                    if (ClientFieldGuideManager.isUnlocked(entry)) {
                        hasUnlocked = true;
                        break;
                    }
                }
            } else {
                hasUnlocked = true;
            }

            if ((isIntro || (entries != null && !entries.isEmpty())) && hasUnlocked) {
                this.sortedCategories.add(cat);
            }
        }
        this.sortedCategories.sort(Comparator.comparingInt(Category::getSortIndex)
                .thenComparing(c -> c.getId().getPath()));

        int maxStart = Math.max(0, this.sortedCategories.size() - MAX_TABS);
        tabStartIndex = Math.max(0, Math.min(tabStartIndex, maxStart));

        this.tabButtons.clear();
        int xPos = this.bounds.left() - 8;

        // Generate all tab widgets
        for (Category category : sortedCategories) {
            TabButton tab = new TabButton(xPos, 0, TAB_WIDTH, TAB_HEIGHT, category, this);
            this.tabButtons.add(tab);
            this.addRenderableWidget(tab);
        }

        this.tabUpButton = new ImageButton(
                this.bounds.left() - 24, this.bounds.top() + 10,
                24, 20,
                ClientConstants.TAB_UP_SPRITES,
                (b) -> scrollTabs(-1)
        );

        this.tabDownButton = new ImageButton(
                this.bounds.left() - 24, this.bounds.bottom() - 30,
                24, 20,
                ClientConstants.TAB_DOWN_SPRITES,
                (b) -> scrollTabs(1)
        );

        this.addRenderableWidget(tabUpButton);
        this.addRenderableWidget(tabDownButton);
        updateTabVisibility();
    }

    private void scrollTabs(int direction) {
        tabStartIndex += direction;
        int maxStart = Math.max(0, this.sortedCategories.size() - MAX_TABS);
        tabStartIndex = Math.max(0, Math.min(tabStartIndex, maxStart));
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        boolean needsPagination = this.sortedCategories.size() > MAX_TABS;

        this.tabUpButton.visible = needsPagination && tabStartIndex > 0;
        this.tabDownButton.visible = needsPagination && tabStartIndex < this.sortedCategories.size() - MAX_TABS;

        int startY = this.bounds.top() + TAB_Y_OFFSET;

        for (int i = 0; i < this.tabButtons.size(); i++) {
            TabButton tab = this.tabButtons.get(i);

            if (i >= tabStartIndex && i < tabStartIndex + MAX_TABS) {
                tab.visible = true;
                tab.active = true;

                int relativeIndex = i - tabStartIndex;
                tab.setY(startY + (relativeIndex * (TAB_HEIGHT + TAB_GAP)));
            } else {
                tab.visible = false;
                tab.active = false;
            }
        }
    }

    abstract public void onTabClick(Category category);

    public void renderFieldGuideBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PUT, 1.0F, 1.0F));
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (FieldGuideClient.OPEN_GUIDE_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public List<Category> getSortedCategories() {
        return this.sortedCategories;
    }

    public Category getSelectedCategory() {
        return this.selectedCategory;
    }

    public void setSelectedCategory(Category category) {
        this.selectedCategory = category;
    }
}
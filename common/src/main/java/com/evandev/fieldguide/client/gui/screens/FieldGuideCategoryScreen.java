package com.evandev.fieldguide.client.gui.screens;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.data.EntryVisual;
import com.evandev.fieldguide.client.gui.util.Bounds;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.client.gui.widget.FieldGuideSearchBox;
import com.evandev.fieldguide.client.gui.widget.PageTurnButton;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.util.DummyEntities;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class FieldGuideCategoryScreen extends BookScreen {
    private static final int ITEMS_PER_PAGE = 9;
    private static final int ITEMS_PER_VIEW = ITEMS_PER_PAGE * 2;

    private static final int GRID_COLS = 3;
    private static final int CELL_SIZE = 40;
    private static final int GAP = 1;

    private static final int SEARCH_WIDTH = 140;
    private static final int SEARCH_HEIGHT = 20;
    public static int lastOpenedJournalPage = 0;
    private static Registry<Biome> biomeRegistry;
    private static Identifier lastOpenedCategory = null;
    private final Map<Identifier, Entity> entryCache = new HashMap<>();
    public boolean isSearching = false;
    private boolean initialSearchFocus = false;
    private ItemStack searchItemStack;
    private List<Object> currentEntries = new ArrayList<>();
    private List<Object> recentEntries = new ArrayList<>();
    private int currentPage = 0;
    private Screen parent = null;
    private String searchQuery = "";
    private PageTurnButton prevPageButton;
    private PageTurnButton nextPageButton;
    private PageTurnButton backButton;
    private FieldGuideSearchBox searchBox;

    public FieldGuideCategoryScreen() {
        super(Component.translatable("title.fieldguide.field_guide"));
    }

    public FieldGuideCategoryScreen(Category initialCategory) {
        this();
        this.setSelectedCategory(initialCategory);
    }

    public FieldGuideCategoryScreen(Category initialCategory, int initialPage) {
        this(initialCategory);
        this.currentPage = initialPage;
    }

    public FieldGuideCategoryScreen(String searchQuery, Screen parent) {
        this();
        this.searchQuery = searchQuery;
        this.parent = parent;
    }

    public static int getPageForEntry(Category category, Object entry) {
        List<Object> entries = ClientFieldGuideManager.getInstance().getEntriesForCategory(category);
        int index = entries.indexOf(entry);
        if (index < 0) return 0;
        return 1 + index / ITEMS_PER_VIEW;
    }

    public void setInitialSearchFocus(boolean focus) {
        this.initialSearchFocus = focus;
    }

    public void setSearchItemStack(ItemStack searchItemStack) {
        this.searchItemStack = searchItemStack;
    }

    public List<Object> getCurrentEntries() {
        return this.currentEntries;
    }

    @Override
    protected void init() {
        if (this.getSelectedCategory() == null && this.searchQuery.isEmpty()) {
            String defaultMode = ClientConfig.get().defaultScreen;
            if ("current_biome".equals(defaultMode) && this.minecraft.level != null && this.minecraft.player != null) {
                var biomeOpt = this.minecraft.level.getBiome(this.minecraft.player.blockPosition()).unwrapKey();
                biomeOpt.ifPresent(biomeResourceKey -> this.searchQuery = "=!" + biomeResourceKey.identifier());
            } else if (!"last_opened_category".equals(defaultMode) && !defaultMode.isEmpty()) {
                Identifier catId = Identifier.tryParse(defaultMode);
                if (catId != null) {
                    Category cat = ClientFieldGuideManager.getCategories().get(catId);
                    if (cat != null) {
                        this.setSelectedCategory(cat);
                    }
                }
            }
        }

        super.init();

        if (this.minecraft.level != null) {
            biomeRegistry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME);
        }

        if (this.getSelectedCategory() == null && this.searchQuery.isEmpty()) {
            if (lastOpenedCategory != null) {
                this.setSelectedCategory(ClientFieldGuideManager.getCategories().get(lastOpenedCategory));
            }

            if (this.getSelectedCategory() == null) {
                Category intro = ClientFieldGuideManager.getCategories().values().stream()
                        .filter(cat -> cat.getId().getPath().equals("intro"))
                        .findFirst()
                        .orElse(null);

                if (intro != null) {
                    this.setSelectedCategory(intro);
                } else if (!this.getSortedCategories().isEmpty()) {
                    this.setSelectedCategory(this.getSortedCategories().getFirst());
                }
            }
        }

        this.isSearching = !this.searchQuery.trim().isEmpty();

        if (!this.isSearching && this.getSelectedCategory() != null && this.getSelectedCategory().getId().getPath().equals("intro")) {
            Objects.requireNonNull(this.minecraft).setScreen(new FieldGuideJournalScreen(this.getSelectedCategory(), lastOpenedJournalPage));
            return;
        }

        if (this.getSelectedCategory() != null) {
            lastOpenedCategory = this.getSelectedCategory().getId();
        }

        // Pagination Buttons
        this.prevPageButton = new PageTurnButton(
                this.bounds.left() + 15,
                this.leftPageBounds.bottom() - 15,
                16,
                16,
                ClientConstants.PREV_PAGE_SPRITES,
                b -> prevPage()
        );

        this.nextPageButton = new PageTurnButton(
                this.bounds.right() - 30,
                this.rightPageBounds.bottom() - 15,
                16,
                16,
                ClientConstants.NEXT_PAGE_SPRITES,
                b -> nextPage()
        );

        this.addRenderableWidget(prevPageButton);
        this.addRenderableWidget(nextPageButton);

        // Back Button
        this.backButton = new PageTurnButton(
                this.bounds.right() - 13,
                this.bounds.top() + 26,
                24,
                24,
                ClientConstants.BACK_SPRITES,
                b -> {
                    if (parent != null) {
                        Objects.requireNonNull(this.minecraft).setScreen(parent);
                    } else {
                        this.searchBox.setValue("");
                    }
                }
        );

        backButton.visible = false;
        this.addRenderableWidget(backButton);

        // Search Bar
        int searchX = this.width / 2 - SEARCH_WIDTH / 2;
        int searchY = this.bounds.bottom() + 5;

        this.searchBox = new FieldGuideSearchBox(this.font, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, this.searchQuery, this::onSearchChanged);
        if (this.initialSearchFocus) {
            this.searchBox.setInitialFocus();
            this.setFocused(this.searchBox);
        }
        this.addRenderableWidget(this.searchBox);

        if (this.isSearching) {
            this.setSelectedCategory(null);
            this.getEntriesForSearchQuery();
        } else {
            this.getEntriesForSelectedCategory();
        }

        int totalSpreads = getTotalSpreads();
        if (this.currentPage >= totalSpreads) {
            this.currentPage = Math.max(0, totalSpreads - 1);
        }

        goToPage(this.currentPage);
    }

    private void onSearchChanged(String query) {
        if (query.equals(this.searchQuery)) return;

        this.isSearching = !query.trim().isEmpty();
        this.searchQuery = query;
        this.searchItemStack = null;

        if (!isSearching) {
            Category category = ClientFieldGuideManager.getCategories().get(lastOpenedCategory);
            if (category != null && category.getId().getPath().equals("intro")) {
                Objects.requireNonNull(this.minecraft).setScreen(new FieldGuideJournalScreen(category, lastOpenedJournalPage));
                return;
            }
            this.setSelectedCategory(category);
            if (category != null) {
                this.getEntriesForSelectedCategory();
            }
        } else {
            this.setSelectedCategory(null);
            this.getEntriesForSearchQuery();
        }

        this.goToPage(0);
    }

    private void getEntriesForSelectedCategory() {
        Category category = this.getSelectedCategory();
        if (category != null) {
            this.currentEntries = ClientFieldGuideManager.getInstance().getEntriesForCategory(category);
            this.recentEntries = ClientFieldGuideManager.getInstance().getRecentEntries(category, 9);
        } else {
            this.currentEntries = new ArrayList<>();
            this.recentEntries = new ArrayList<>();
        }
    }

    private void getEntriesForSearchQuery() {
        this.currentEntries = ClientFieldGuideManager.getInstance().searchEntries(this.searchQuery);
        this.recentEntries = new ArrayList<>();
    }

    @Override
    public void onTabClick(Category category) {
        lastOpenedCategory = category.getId();

        if (category.getId().getPath().equals("intro")) {
            Objects.requireNonNull(this.minecraft).setScreen(new FieldGuideJournalScreen(category, lastOpenedJournalPage));
            return;
        }

        this.setSelectedCategory(category);
        this.getEntriesForSelectedCategory();
        this.goToPage(0);

        if (this.searchBox != null) {
            this.searchBox.setValue("");
            this.isSearching = false;
        }

        if (!this.children().isEmpty()) {
            this.rebuildWidgets();
        }
    }

    private int getTotalSpreads() {
        int count = currentEntries.size();
        if (isSearching) {
            if (count == 0) return 1;
            return (int) Math.ceil((double) count / ITEMS_PER_VIEW);
        }

        if (count == 0) return 1;
        return 1 + (int) Math.ceil((double) count / ITEMS_PER_VIEW);
    }

    private void updatePageButtons() {
        int totalSpreads = getTotalSpreads();
        this.prevPageButton.visible = currentPage > 0;
        this.nextPageButton.visible = currentPage < totalSpreads - 1;
    }

    private void goToPage(int page) {
        this.currentPage = page;
        this.updatePageButtons();
    }

    private void prevPage() {
        if (currentPage > 0) {
            this.goToPage(currentPage - 1);
        }
    }

    private void nextPage() {
        if (currentPage < getTotalSpreads() - 1) {
            this.goToPage(currentPage + 1);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (this.searchBox != null) this.searchBox.setFocused(this.searchBox.isMouseOver(event.x(), event.y()));
        if (super.mouseClicked(event, doubleClick)) return true;

        if (!isSearching && currentPage == 0) {
            for (int i = 0; i < recentEntries.size(); i++) {
                int slotIndex = ITEMS_PER_PAGE + i;
                Bounds bounds = getGridCellBounds(slotIndex);

                if (bounds.contains((int) event.x(), (int) event.y())) {
                    Object entry = recentEntries.get(i);
                    handleEntryClick(entry);
                    return true;
                }
            }
            return false;
        }

        for (int i = 0; i < ITEMS_PER_VIEW; i++) {
            if (!isSearching && currentPage == 0) continue;

            Bounds cellBounds = getGridCellBoundsLocal(i);

            if (cellBounds.contains((int) event.x(), (int) event.y())) {
                int itemIndex = getItemIndexForSlot(i);
                if (itemIndex >= 0 && itemIndex < currentEntries.size()) {
                    handleEntryClick(currentEntries.get(itemIndex));
                    return true;
                }
            }
        }
        return false;
    }

    private void handleEntryClick(Object entry) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));

        EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(entry);
        Object coreEntry = EntryResolver.resolveCoreEntry(entry);
        boolean isCobblemon = entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "cobblemon".equals(ge.virtualData().virtualType());

        if (coreEntry instanceof EntityType<?> || isCobblemon) {
            Entity entity = getCachedEntity(entry);

            if (entity != null && ClientFieldGuideManager.isUnlocked(entry)) {
                if (visual != null && visual.customSound != null) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(visual.customSound), 1.0F, 1.0F));
                } else {
                    FieldGuideClient.playMobCry(entity);
                }
            }
        } else if (coreEntry instanceof Block && ClientFieldGuideManager.isUnlocked(entry)) {
            if (visual != null && visual.customSound != null) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(visual.customSound), 1.0F, 1.0F));
            }
        }

        Minecraft.getInstance().setScreen(new FieldGuideEntryScreen(this, entry));
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.searchBox.isFocused()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            if (this.searchBox.keyPressed(event)) return true;
            return true;
        }

        if (this.minecraft.player != null && this.minecraft.options.keyInventory.matches(event)) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (scrollY > 0) {
            this.prevPage();
            return true;
        } else if (scrollY < 0) {
            this.nextPage();
            return true;
        }

        return false;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractFieldGuideBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().pushMatrix();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.BOOK_TEXTURE, this.bounds.left(), this.bounds.top(), 0f, 0f, this.bounds.width(), this.bounds.height(), 300, 200);

        // Page Numbers
        int leftPageNum, rightPageNum;
        leftPageNum = (currentPage + 1) * 2 - 1;
        rightPageNum = (currentPage + 1) * 2;

        if (!isSearching) {
            leftPageNum -= 2;
            rightPageNum -= 2;
        }

        if (currentPage > 0 || isSearching) {
            int titleY = this.rightPageBounds.top() + 8;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_PAGE_TEXTURE, this.bounds.left(), this.bounds.top(), 0f, 0f, this.bounds.width(), this.bounds.height(), 300, 200);
            renderPageNumber(leftPageNum, this.leftPageBounds, guiGraphics);
            if (isSearching) {
                if (currentEntries.size() > leftPageNum * ITEMS_PER_PAGE) {
                    renderPageNumber(rightPageNum, this.rightPageBounds, guiGraphics);
                }

                // Biome Title
                if (searchQuery.startsWith("=!")) {
                    Identifier biomeId = Identifier.tryParse(searchQuery.substring(2));
                    if (biomeId != null && biomeRegistry.containsKey(biomeId)) {
                        int titleColor = ClientConfig.get().getTextColorInt();
                        int iconOffset = 0;

                        if (Services.PLATFORM.isModLoaded("immersiveoverlays")) {
                            Identifier texture = Identifier.fromNamespaceAndPath(biomeId.getNamespace(), "textures/immersiveoverlays/" + biomeId.getPath() + ".png");
                            if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                                int iconSize = 16;
                                int iconY = titleY - 5;
                                int iconX = this.leftPageBounds.left() + 3;
                                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
                                iconOffset = iconSize;
                            }
                        }

                        Component searchTitle = Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());
                        renderTitle(guiGraphics, searchTitle, iconOffset, titleColor);
                    } else {
                        renderTitle(guiGraphics, Component.translatable("gui.fieldguide.searching_biomes"));
                    }
                } else if (searchQuery.startsWith("=#")) {
                    renderTitle(guiGraphics, Component.literal("#" + searchQuery.substring(2)), 0, ClientConfig.get().getTextColorInt());
                } else if (searchQuery.startsWith("=$")) {
                    String seasonId = searchQuery.substring(2).toLowerCase(Locale.ROOT);
                    Season season = null;
                    for (Season s : Season.values()) {
                        if (s.getId().equals(seasonId)) {
                            season = s;
                            break;
                        }
                    }

                    if (season != null) {
                        Component seasonName = season.getDisplayName();
                        renderTitle(guiGraphics, Component.translatable("gui.fieldguide.grows_in", seasonName), 0, ClientConfig.get().getTextColorInt());
                    } else {
                        renderTitle(guiGraphics, Component.translatable("gui.fieldguide.searching_seasons"));
                    }
                } else if (searchQuery.startsWith("=^")) {
                    String dropQuery = searchQuery.substring(2).toLowerCase(Locale.ROOT);
                    ItemStack displayStack = ItemStack.EMPTY;
                    String dropName = searchQuery.substring(2);

                    if (this.searchItemStack != null) {
                        displayStack = this.searchItemStack;
                        dropName = displayStack.getHoverName().getString();
                    } else {
                        for (Object entry : currentEntries) {
                            List<ItemStack> drops = ClientFieldGuideManager.getInstance().getDrops(entry);

/*                            if (drops.isEmpty() && entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "cobblemon".equals(ge.virtualData().virtualType())) {
                                drops = ClientFieldGuideCobblemonCompat.getCobblemonDrops(entry);
                            }*/

                            for (ItemStack stack : drops) {
                                if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).equals(dropQuery)) {
                                    displayStack = stack;
                                    dropName = stack.getHoverName().getString();
                                    break;
                                }
                            }
                            if (!displayStack.isEmpty()) break;
                        }
                    }

                    int iconSize = 16;
                    int iconY = titleY - 5;
                    int iconX = this.leftPageBounds.left() + 3;

                    if (!displayStack.isEmpty()) {
                        guiGraphics.fakeItem(displayStack, iconX, iconY);
                        renderTitle(guiGraphics, Component.translatable("gui.fieldguide.drops", dropName), iconSize, ClientConfig.get().getTextColorInt());
                    } else {
                        renderTitle(guiGraphics, Component.translatable("gui.fieldguide.searching_drops"));
                    }
                } else {
                    Component title = getSearchTitle();
                    renderTitle(guiGraphics, title);
                }
            } else {
                int startIdx = (currentPage - 1) * ITEMS_PER_VIEW;
                if (currentEntries.size() > startIdx + ITEMS_PER_PAGE) {
                    renderPageNumber(rightPageNum, this.rightPageBounds, guiGraphics);
                }

                // Category Title
                Component title = Component.translatable(this.getSelectedCategory().getTranslationKey());
                renderTitle(guiGraphics, title);
            }
        }

        if (!isSearching && this.getSelectedCategory() != null && currentPage == 0) {
            renderCategoryInfo(guiGraphics);
            renderRecentDiscoveries(guiGraphics, mouseX, mouseY);
        } else if (isSearching) {
            if (currentEntries.isEmpty()) {
                Component noResults = Component.translatable("gui.fieldguide.no_results");
                guiGraphics.text(this.font, noResults, this.leftPageBounds.x_center() - this.font.width(noResults) / 2, this.leftPageBounds.y_center() - (font.lineHeight / 2), ClientConfig.get().getTextMutedColorInt(), false);
            }
        }

        // Back Button
        this.backButton.visible = this.isSearching;
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // Grid
        if (currentPage > 0 || isSearching) {
            renderGrid(guiGraphics, mouseX, mouseY);
        }

        guiGraphics.pose().popMatrix();
    }

    private @NotNull Component getSearchTitle() {
        Component title = Component.translatable("gui.fieldguide.searching_name");
        if (searchQuery.startsWith("^")) {
            title = Component.translatable("gui.fieldguide.searching_drops");
        } else if (searchQuery.startsWith("!")) {
            title = Component.translatable("gui.fieldguide.searching_biomes");
        } else if (searchQuery.startsWith("@")) {
            title = Component.translatable("gui.fieldguide.searching_mod");
        } else if (searchQuery.startsWith("#")) {
            title = Component.translatable("gui.fieldguide.searching_tags");
        }
        return title;
    }

    private void renderTitle(GuiGraphicsExtractor guiGraphics, Component text) {
        renderTitle(guiGraphics, text, 0, ClientConfig.get().getTextMutedColorInt());
    }

    private void renderTitle(GuiGraphicsExtractor guiGraphics, Component text, int offset, int color) {
        int titleY = this.rightPageBounds.top() + 8;

        int maxWidth = this.leftPageBounds.width() - 12 - offset;
        String title = text.getString();
        if (this.font.width(text) > maxWidth) {
            title = font.plainSubstrByWidth(title, maxWidth) + "...";
        }
        guiGraphics.text(this.font, title, this.leftPageBounds.left() + 6 + offset, titleY, color, false);
    }

    private void renderCategoryInfo(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.TITLE_PAGE_TEXTURE, this.bounds.left(), this.bounds.top(), 0f, 0f, this.bounds.width(), this.bounds.height(), 300, 200);

        Category category = this.getSelectedCategory();
        Component title = Component.translatable(category.getTranslationKey());
        int titleY = this.leftPageBounds.top() + 36;
        List<FormattedCharSequence> lines = this.font.split(title, 70);

        if (lines.size() > 2) {
            titleY -= font.lineHeight;
        }

        for (FormattedCharSequence line : lines) {
            int lineWidth = this.font.width(line);
            int lineX = this.leftPageBounds.x_center() - lineWidth / 2;
            guiGraphics.text(this.font, line, lineX, titleY, ClientConfig.get().getTextTitleColorInt(), false);
            titleY += font.lineHeight;
        }

        int total = currentEntries.size();
        if (total > 0) {
            long unlocked = currentEntries.stream().filter(ClientFieldGuideManager::isUnlocked).count();
            int y = this.leftPageBounds.bottom() - 38;
            int x = this.leftPageBounds.x_center();
            int xOffset = 14;

            String countText = String.valueOf(unlocked);
            String totalText = String.valueOf(total);

            guiGraphics.text(this.font, countText, x - xOffset - font.width(countText) / 2, y, ClientConfig.get().getTextColorInt(), false);
            guiGraphics.text(this.font, totalText, x + xOffset - font.width(totalText) / 2, y, ClientConfig.get().getTextColorInt(), false);

            // Progress Bar
            if (unlocked > 0) {
                int barWidth = 83;
                int barHeight = 6;
                int barX = x - barWidth / 2 - 1;
                int barY = y + 15;

                int progressWidth = (int) ((float) unlocked / total * barWidth);
                progressWidth = Math.max(progressWidth, 6);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "widget/progress_bar_fill"), barX, barY, progressWidth, barHeight);
            }
        }
    }

    private void renderRecentDiscoveries(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Title
        Component title = Component.translatable("gui.fieldguide.main.default");
        int titleY = this.rightPageBounds.top() + 8;
        guiGraphics.text(this.font, title, this.rightPageBounds.x_center() - font.width(title) / 2, titleY, ClientConfig.get().getTextMutedColorInt(), false);

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            if (i >= recentEntries.size()) break;
            Object entry = recentEntries.get(i);
            int slotIndex = ITEMS_PER_PAGE + i;
            Bounds bounds = getGridCellBoundsLocal(slotIndex);

            boolean hovered = bounds.contains(mouseX, mouseY);
            if (hovered) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_ENTRY_BACKGROUND_TEXTURE, bounds.x(), bounds.y(), 0f, (float) CELL_SIZE, CELL_SIZE, CELL_SIZE, 300, 200);
            } else {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_ENTRY_BACKGROUND_TEXTURE, bounds.x(), bounds.y(), 0f, 0f, CELL_SIZE, CELL_SIZE, 300, 200);
            }

            renderEntryInGrid(guiGraphics, entry, bounds.x_center(), bounds.y_center(), true);

            if (ClientFieldGuideManager.isNew(entry)) {
                renderNewLabel(guiGraphics, bounds);
            }
        }

        // Tooltips
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            if (i >= recentEntries.size()) break;
            Object entry = recentEntries.get(i);
            int slotIndex = ITEMS_PER_PAGE + i;
            Bounds bounds = getGridCellBoundsLocal(slotIndex);
            if (bounds.contains(mouseX, mouseY)) {
                renderEntryTooltip(guiGraphics, entry, mouseX, mouseY, true);
            }
        }
    }

    private int getItemIndexForSlot(int slotIndex) {
        if (isSearching) return (currentPage * ITEMS_PER_VIEW) + slotIndex;
        if (currentPage == 0) return -1;

        int startItemIndex = (currentPage - 1) * ITEMS_PER_VIEW;
        return startItemIndex + slotIndex;
    }

    private void renderGrid(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < ITEMS_PER_VIEW; i++) {
            int itemIndex = getItemIndexForSlot(i);
            if (itemIndex >= 0 && itemIndex < currentEntries.size()) {
                Object entry = currentEntries.get(itemIndex);
                Bounds bounds = getGridCellBoundsLocal(i);
                boolean unlocked = ClientFieldGuideManager.isUnlocked(entry);
                boolean hovered = bounds.contains(mouseX, mouseY);

                if (hovered) {
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_ENTRY_BACKGROUND_TEXTURE, bounds.x(), bounds.y(), 0f, (float) CELL_SIZE, CELL_SIZE, CELL_SIZE, 300, 200);
                } else {
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_ENTRY_BACKGROUND_TEXTURE, bounds.x(), bounds.y(), 0f, 0f, CELL_SIZE, CELL_SIZE, 300, 200);
                }

                renderEntryInGrid(guiGraphics, entry, bounds.x_center(), bounds.y_center(), unlocked);

                if (ClientFieldGuideManager.isNew(entry)) {
                    renderNewLabel(guiGraphics, bounds);
                }
            }
        }

        for (int i = 0; i < ITEMS_PER_VIEW; i++) {
            int itemIndex = getItemIndexForSlot(i);
            if (itemIndex >= 0 && itemIndex < currentEntries.size()) {
                Bounds bounds = getGridCellBoundsLocal(i);
                if (bounds.contains(mouseX, mouseY)) {
                    Object entry = currentEntries.get(itemIndex);
                    boolean unlocked = ClientFieldGuideManager.isUnlocked(entry);
                    renderEntryTooltip(guiGraphics, entry, mouseX, mouseY, unlocked);
                }
            }
        }
    }

    private Entity getCachedEntity(Object entry) {
        Identifier id = ClientFieldGuideManager.getEntryId(entry);
        if (id == null) return null;

        Entity entity = entryCache.get(id);
        if (entity == null) {
            if (this.minecraft.level == null) return null;

            /*if (entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "cobblemon".equals(ge.virtualData().virtualType())) {
                entity = ClientFieldGuideCobblemonCompat.getDummyPokemon(id, this.minecraft.level);
            }*/
            Object coreEntry = EntryResolver.resolveCoreEntry(entry);
            if (coreEntry instanceof EntityType<?> type) {
                try {
                    entity = DummyEntities.create(type, this.minecraft.level, EntitySpawnReason.LOAD);
                    if (Services.PLATFORM.isModLoaded("mixed_litter")) {
                        Services.PLATFORM.applyMixedLitterCompat(entity);
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Failed to create entity for guide: {}", type.getDescription().getString());
                }
            }

            if (entity != null) entryCache.put(id, entity);
        }

        if (entity instanceof Mob mob) {
            String selectedVariant = ProgressManager.getInstance().getSelectedVariant(entry);
            if (selectedVariant != null) {
                VariantProvider<Mob> provider = FieldGuideVariantManager.getProvider(mob);
                if (provider != null) {
                    VariantDef current = provider.getCurrent(mob);
                    if (!current.id().equals(selectedVariant)) {
                        List<VariantDef> variants = provider.getVariants(mob);
                        for (VariantDef variant : variants) {
                            if (variant.id().equals(selectedVariant)) {
                                provider.apply(mob, variant);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return entity;
    }

    private void renderNewLabel(GuiGraphicsExtractor guiGraphics, Bounds bounds) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0f, 0f);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.LIST_ENTRY_NEW_TEXTURE, bounds.x(), bounds.y(), 0f, 0f, CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE);
        guiGraphics.pose().popMatrix();
    }

    private void renderEntryTooltip(GuiGraphicsExtractor guiGraphics, Object entry, int mouseX, int mouseY, boolean unlocked) {
        if (unlocked || ServerConfig.get().showUndiscoveredNames) {
            Component name = ClientFieldGuideManager.getEntryName(entry);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(name);

            Object coreEntry = EntryResolver.resolveCoreEntry(entry);
            Entity dummy = getCachedEntity(entry);

            boolean isCobblemon = entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "cobblemon".equals(ge.virtualData().virtualType());

            if (dummy != null && (coreEntry instanceof EntityType<?> || isCobblemon)) {
                List<VariantDef> variants = FieldGuideVariantManager.getVariants(dummy);
                if (variants.size() > 1) {
                    int unlockedCount = ServerConfig.get().unlockAllVariants ? variants.size() : 0;

                    if (!ServerConfig.get().unlockAllVariants) {
                        for (VariantDef var : variants) {
                            if (ClientFieldGuideManager.isVariantUnlocked(entry, var.id())) {
                                unlockedCount++;
                            }
                        }
                    }
                    tooltip.add(Component.translatable("fieldguide.tooltip.variants", unlockedCount, variants.size()).withStyle(ChatFormatting.GRAY));
                }
            }

            if (this.minecraft.options.advancedItemTooltips) {
                Identifier id = ClientFieldGuideManager.getEntryId(entry);
                if (id != null) {
                    tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            guiGraphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        } else {
            guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("fieldguide.unknown"), mouseX, mouseY);
        }
    }

    private Bounds getGridCellBounds(int globalSlotIndex) {
        return getGridCellBoundsLocal(globalSlotIndex % ITEMS_PER_VIEW);
    }

    private Bounds getGridCellBoundsLocal(int i) {
        Bounds pageBounds;
        if (i < ITEMS_PER_PAGE) pageBounds = this.leftPageBounds;
        else pageBounds = this.rightPageBounds;

        int startX = pageBounds.left() + 6;
        int startY = pageBounds.top() + 24;
        int localIndex = i % ITEMS_PER_PAGE;
        int col = localIndex % GRID_COLS;
        int row = localIndex / GRID_COLS;
        int x = startX + (col * (CELL_SIZE + GAP));
        int y = startY + (row * (CELL_SIZE + GAP));
        return new Bounds(x, y, CELL_SIZE, CELL_SIZE);
    }

    private void renderPageNumber(int page, Bounds bounds, GuiGraphicsExtractor guiGraphics) {
        String str = page + "";
        guiGraphics.text(this.font, str, bounds.x_center() - font.width(str) / 2, bounds.bottom() - 11, ClientConfig.get().getPageNumberColorInt(), false);
    }

    private void renderEntryInGrid(GuiGraphicsExtractor guiGraphics, Object entry, int x, int y, boolean unlocked) {
        Object coreEntry = EntryResolver.resolveCoreEntry(entry);
        boolean isCobblemon = entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "cobblemon".equals(ge.virtualData().virtualType());
        boolean isTutorial = entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "tutorial".equals(ge.virtualData().virtualType());

/*        if (unlocked && Services.PLATFORM.isModLoaded("exposure") && ClientConfig.get().exposureShowPhotographsInGrid) {
            String selectedVariant = ProgressManager.getInstance().getSelectedVariant(entry);
            ItemStack existingPhoto = ProgressManager.getInstance().getPhotograph(entry, selectedVariant);
            if (!existingPhoto.isEmpty()) {
                ClientExposureCompat.renderPhotographInGrid(guiGraphics, x - (CELL_SIZE / 2), y - (CELL_SIZE / 2), CELL_SIZE, CELL_SIZE, existingPhoto);
                return;
            } else if (ServerConfig.get().keepSilhouetteWhenUnlocked) {
                ClientExposureCompat.renderMissingPhotoBackground(guiGraphics, x - (CELL_SIZE / 2), y - (CELL_SIZE / 2), CELL_SIZE, CELL_SIZE);
            }
        }*/

        if (entry instanceof GuideEntry ge && ge.isStructure() && coreEntry instanceof Block) {
            EntryRenderHelper.renderStructure(guiGraphics, ge, x, y, CELL_SIZE - 4, unlocked, false, 1.0F);
        }// else if (isCobblemon) {
        // EntryRenderHelper.renderCobblemon(guiGraphics, (GuideEntry) entry, x, y, CELL_SIZE - 8, CELL_SIZE - 8, unlocked, false, 1.0F);
        //}
        else if (isTutorial) {
            EntryRenderHelper.renderTutorial(guiGraphics, (GuideEntry) entry, x, y, CELL_SIZE - 8, CELL_SIZE - 8, unlocked, false, 1.0F);
        } else if (coreEntry instanceof EntityType<?>) {
            Entity entity = getCachedEntity(entry);
            if (entity != null) {
                EntryRenderHelper.renderEntityNormalized(guiGraphics, entity, x, y, CELL_SIZE - 8, CELL_SIZE - 8, unlocked, false, 1.0F);
            }
        } else if (coreEntry instanceof Block block) {
            EntryRenderHelper.renderBlock(guiGraphics, block, x, y, 15.0F, unlocked, false, 1.0F);
        } else if (coreEntry instanceof Item item) {
            EntryRenderHelper.renderItem(guiGraphics, item, x, y, 20.0F, unlocked, false, 1.0F);
        }
    }
}
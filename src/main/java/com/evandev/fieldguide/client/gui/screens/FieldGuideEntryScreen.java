package com.evandev.fieldguide.client.gui.screens;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.FieldGuideLimits;
import com.evandev.fieldguide.ModDataComponents;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.attribute.AttributeRegistry;
import com.evandev.fieldguide.api.attribute.GuideAttribute;
import com.evandev.fieldguide.api.seasons.Season;
import com.evandev.fieldguide.api.seasons.SeasonsAPI;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.FieldGuideClient;
import com.evandev.fieldguide.client.data.EntryVisual;
import com.evandev.fieldguide.client.gui.util.Bounds;
import com.evandev.fieldguide.client.gui.util.ClickInput;
import com.evandev.fieldguide.client.gui.util.EntryAttributesRenderer;
import com.evandev.fieldguide.client.gui.util.EntryBiomeWidgetHelper;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import com.evandev.fieldguide.client.gui.util.KeyInput;
import com.evandev.fieldguide.client.gui.widget.*;
import com.evandev.fieldguide.client.manager.ClientTextManager;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.exposure.ClientExposureCompat;
import com.evandev.fieldguide.compat.nomansland.NoMansLandCompat;
import com.evandev.fieldguide.compat.scholar.ScholarCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.CopyPagePacket;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.util.DummyEntities;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}

public class FieldGuideEntryScreen extends BookScreen {
    private static final int LINE_HEIGHT = 9;
    private final FieldGuideCategoryScreen parent;
    private final Object entry;
    private final List<ResourceLocation> spawnBiomes = new ArrayList<>();
    private final List<AbstractWidget> exposureWidgets = new ArrayList<>();
    private final Map<String, Entity> variantEntityCache = new HashMap<>();
    private List<ItemStack> loadedDrops = new ArrayList<>();
    private boolean dataLoaded = false;
    private boolean isLoadingData = false;
    private String initialVariant = null;
    private Entity renderedEntity;
    private long lastClickTime = 0;
    private int currentVariantIndex = 0;
    private List<VariantDef> entityVariants = new ArrayList<>();
    private PageTurnButton prevVariantButton;
    private PageTurnButton nextVariantButton;
    private VariantOverviewWidget variantOverviewWidget;
    private FriendMoonWidget friendMoonWidget;
    private int descX, descY, descW;
    private boolean replayResetForScreen = false;

    private float hoverScale = 1.0f;
    private long lastRenderTime = 0;

    private AbstractWidget descriptionWidget;
    private List<GuideAttribute> activeAttributes = new ArrayList<>();

    public FieldGuideEntryScreen(FieldGuideCategoryScreen parent, Object entry) {
        super(getTitleForEntry(entry));
        this.parent = parent;
        this.entry = entry;
    }

    private static Component getTitleForEntry(Object entry) {
        if (ClientFieldGuideManager.isUnlocked(entry) || ServerConfig.get().showUndiscoveredNames) {
            return ClientFieldGuideManager.getEntryName(entry);
        }
        return Component.translatable("fieldguide.undiscovered");
    }

    private static @NotNull String getTimeKey(long gameTime) {
        long timeOfDay = gameTime % 24000L;

        String timeKey = "fieldguide.time.day";

        if (timeOfDay >= 4500 && timeOfDay < 7500) {
            timeKey = "fieldguide.time.noon";
        } else if (timeOfDay >= 16500 && timeOfDay < 19500) {
            timeKey = "fieldguide.time.midnight";
        } else if (timeOfDay >= 13000 && timeOfDay < 23000) {
            timeKey = "fieldguide.time.night";
        }
        return timeKey;
    }

    public static boolean isVisualVariantUnlocked(Object entry, VariantDef variant) {
        if (ServerConfig.get().unlockAllVariants) return true;
        return ClientFieldGuideManager.isVariantUnlocked(entry, variant.id());
    }

    private boolean isCobblemon(Object entry) {
        return FieldGuideCobblemonCompat.isCobblemonEntry(entry);
    }

    public void setInitialVariant(String variantId) {
        this.initialVariant = variantId;
    }

    public void refresh() {
        //? if <26.1 {
        if (this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        }
        //?} else {
        /*this.clearWidgets();
        this.init();
        *///?}
    }

    public FieldGuideCategoryScreen getParentScreen() {
        return parent;
    }

    public Bounds getLeftPageBounds() {
        return leftPageBounds;
    }

    public void addExposureWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
        this.exposureWidgets.add(widget);
    }

    private void refreshExposureWidgets() {
        if (Services.PLATFORM.isModLoaded("exposure")) {
            for (AbstractWidget widget : exposureWidgets) {
                this.removeWidget(widget);
            }
            exposureWidgets.clear();

            String variantId = (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) ? entityVariants.get(currentVariantIndex).id() : null;
            ClientExposureCompat.setupExposureWidgets(this, entry, variantId);
        }
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean overviewVisible = this.variantOverviewWidget != null && this.variantOverviewWidget.isVisible();

        for (AbstractWidget widget : exposureWidgets) {
            widget.visible = !overviewVisible;
        }

        if (this.friendMoonWidget != null) {
            this.friendMoonWidget.visible = !overviewVisible;
        }

        if (this.prevVariantButton != null) {
            this.prevVariantButton.visible = !overviewVisible;
            this.prevVariantButton.active = currentVariantIndex > 0;
        }

        if (this.nextVariantButton != null) {
            this.nextVariantButton.visible = !overviewVisible;
            this.nextVariantButton.active = currentVariantIndex < entityVariants.size() - 1;
        }
    }

    @Override
    protected void init() {
        Category category = ClientFieldGuideManager.getInstance().getCategoryForEntry(entry);
        if (category != null) this.setSelectedCategory(category);
        else if (parent != null) this.setSelectedCategory(parent.getSelectedCategory());

        super.init();

        boolean unlocked = ClientFieldGuideManager.isUnlocked(entry);
        if (ClientFieldGuideManager.isNew(entry)) {
            ClientFieldGuideManager.markAsSeen(entry);
        }

        setupEntityPreview();
        setupTextWidgets(unlocked);

        if (unlocked) {
            this.loadedDrops = ClientFieldGuideManager.getInstance().getDrops(entry, this.initialVariant);
        }

        if (!dataLoaded && !isLoadingData) {
            isLoadingData = true;
            CompletableFuture.runAsync(() -> {
                loadSpawnBiomes();
                if (unlocked) {
                    this.loadedDrops = ClientFieldGuideManager.getInstance().getDrops(entry, this.initialVariant);
                }
                if (this.minecraft != null) {
                    this.minecraft.execute(() -> {
                        this.dataLoaded = true;
                        this.isLoadingData = false;
                        this.refresh();
                    });
                }
            });
        }

        if (dataLoaded) {
            setupBiomeWidget(unlocked);
            setupDropWidget(unlocked);
        }

        setupNavigationButtons();
        refreshExposureWidgets();
        setupFriendMoonWidget(unlocked);

        if (unlocked && ServerConfig.get().enableCopyingPages) {
            boolean hasPaper = this.minecraft != null && this.minecraft.player != null && (this.minecraft.player.isCreative() || this.minecraft.player.getInventory().contains(Items.PAPER.getDefaultInstance()));
            boolean canCopy = hasPaper || (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative());

            PageTurnButton copyBtn = new PageTurnButton(this.bounds.right() - 13, this.bounds.bottom() - 56, 24, 24, ClientConstants.COPY_SPRITES, (btn) -> {
                if (canCopy) {
                    ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
                    if (id != null) {
                        Services.NETWORK.sendToServer(new CopyPagePacket(id));
                        this.onClose();
                    }
                }
            }) {
                @Override
                public void playDownSound(SoundManager handler) {
                    handler.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
                }
            };
            copyBtn.active = canCopy;
            this.addRenderableWidget(copyBtn).setTooltip(createCopyTooltip(canCopy));
        }

        if (this.entityVariants.size() > 1) {
            LivingEntity living = this.renderedEntity instanceof LivingEntity ? (LivingEntity) this.renderedEntity : null;
            int widgetWidth = this.leftPageBounds.width();
            int widgetHeight = this.leftPageBounds.height();
            int widgetX = this.leftPageBounds.left();
            int widgetY = this.leftPageBounds.top();
            this.variantOverviewWidget = new VariantOverviewWidget(widgetX, widgetY, widgetWidth, widgetHeight, this.entry, living, this.entityVariants, this::setVariantIndex, this::updateWidgetVisibility);
            this.addRenderableWidget(this.variantOverviewWidget);
            updateWidgetVisibility();
        }
    }

    private void setupFriendMoonWidget(boolean unlocked) {
        this.friendMoonWidget = null;
        if (!replayResetForScreen) {
            NoMansLandCompat.stopReplay();
            replayResetForScreen = true;
        }
        if (!unlocked || !NoMansLandCompat.isAvailable() || !NoMansLandCompat.isIntegrationUnlocked()) return;

        List<ResourceLocation> dialogues = NoMansLandCompat.getDialoguesForEntry(EntryResolver.resolveCoreEntry(entry));
        if (dialogues.isEmpty()) return;

        int iconSize = 16;
        int iconX = this.leftPageBounds.left() + this.leftPageBounds.width() - iconSize - 12;
        int iconY = this.leftPageBounds.top() + 12;

        if (Services.PLATFORM.isModLoaded("exposure")) {
            String variantId = (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size())
                    ? entityVariants.get(currentVariantIndex).id() : null;
            if (!ClientExposureCompat.willRenderAddPhotoButton(entry, variantId)) return;
            iconY += iconSize + 2;
        }

        this.friendMoonWidget = new FriendMoonWidget(iconX, iconY, iconSize, dialogues,
                () -> ClientFieldGuideManager.getEntryDescription(entry, this.initialVariant));
        this.addRenderableWidget(this.friendMoonWidget);
        updateWidgetVisibility();
    }

    private boolean showsUnlockQuill() {
        return ClientConfig.get().showUnlockDate && ProgressManager.getInstance().getDiscoveryTime(entry) > 0;
    }

    private int getNameWidth() {
        int width = this.rightPageBounds.width() - 12;
        return showsUnlockQuill() ? width - 13 : width;
    }

    private void setupTextWidgets(boolean unlocked) {
        int textX = this.rightPageBounds.left() + 6;
        int titleY = this.leftPageBounds.top() + 8;
        int textAreaWidth = this.rightPageBounds.width() - 12;

        if (unlocked) {
            String initialName = ClientFieldGuideManager.getEntryName(entry).getString();
            if (!ServerConfig.get().disableEditingNames) {
                int nameWidth = getNameWidth();
                AbstractWidget nameWidget = ScholarCompat.createTextField(this.font, textX, titleY, nameWidth, LINE_HEIGHT, initialName, ClientConfig.get().getTextTitleColorInt(), nameWidth, FieldGuideLimits.MAX_ENTRY_NAME_LENGTH,
                        newName -> ClientFieldGuideManager.setCustomName(entry, newName), false);
                this.addRenderableWidget(nameWidget);
            }

            int currentY = titleY + LINE_HEIGHT + 2;

            if (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) {
                VariantDef variant = entityVariants.get(currentVariantIndex);
                String variantId = variant.id();
                String customVariantName = ProgressManager.getInstance().getCustomName(ClientFieldGuideManager.getEntryId(entry).toString() + "#" + variantId);
                String defaultVariantName = (variant.value() instanceof EntryVariantData vd && vd.displayName() != null)
                        ? vd.displayName().getString()
                        : FieldGuideVariantManager.getVariantDisplayName(variant).getString();
                String initialVariantName = customVariantName != null ? customVariantName : defaultVariantName;

                AbstractWidget variantWidget = ScholarCompat.createTextField(this.font, textX, currentY, textAreaWidth, LINE_HEIGHT, initialVariantName, ClientConfig.get().getTextMutedColorInt(), textAreaWidth, FieldGuideLimits.MAX_ENTRY_NAME_LENGTH,
                        newName -> {
                            ResourceLocation entryId = ClientFieldGuideManager.getEntryId(entry);
                            if (entryId != null) {
                                ProgressManager.getInstance().setCustomVariantName(entryId, variantId, newName);
                            }
                        }, false);
                this.addRenderableWidget(variantWidget);
                currentY += LINE_HEIGHT;
            }

            this.activeAttributes = AttributeRegistry.getAttributes(entry, renderedEntity);
            if (!activeAttributes.isEmpty()) {
                currentY += (LINE_HEIGHT * 3);
            } else {
                currentY += LINE_HEIGHT;
            }

            int textY = currentY;
            int textAreaHeight = this.rightPageBounds.bottom() - 29 - textY;
            int maxLines = textAreaHeight / LINE_HEIGHT;

            this.descX = textX;
            this.descY = textY;
            this.descW = textAreaWidth;

            String initialDesc = ClientFieldGuideManager.getEntryDescription(entry, this.initialVariant);
            if (!ServerConfig.get().disableEditingDescriptions) {
                this.descriptionWidget = ScholarCompat.createTextArea(this.font, textX, textY, textAreaWidth, textAreaHeight, maxLines, LINE_HEIGHT, ClientConfig.get().getTextColorInt(), true, FieldGuideLimits.MAX_ENTRY_DESCRIPTION_LENGTH, initialDesc,
                        newDesc -> ClientFieldGuideManager.setCustomDescription(entry, this.initialVariant, newDesc));
                this.addRenderableWidget(this.descriptionWidget);
            }
        }
    }

    private void setupEntityPreview() {
        if (this.minecraft == null || this.minecraft.level == null) return;

        this.entityVariants = new ArrayList<>();

        if (entry instanceof GuideEntry ge && ge.visualVariants() != null && !ge.visualVariants().isEmpty()) {
            for (EntryVariantData vd : ge.visualVariants()) {
                this.entityVariants.add(new VariantDef(vd.variantId(), vd));
            }

            if (this.entityVariants.size() > 1) {
                int centerX = leftPageBounds.x_center();
                int centerY = leftPageBounds.y_center() - 15;

                this.prevVariantButton = new PageTurnButton(centerX - 70, centerY - 8, 16, 16, ClientConstants.PREV_SPRITES, b -> cycleVariant(-1));
                this.nextVariantButton = new PageTurnButton(centerX + 54, centerY - 8, 16, 16, ClientConstants.NEXT_SPRITES, b -> cycleVariant(1));

                this.addRenderableWidget(prevVariantButton);
                this.addRenderableWidget(nextVariantButton);

                if (this.initialVariant == null) {
                    this.initialVariant = ProgressManager.getInstance().getSelectedVariant(entry);
                    if (this.initialVariant == null) this.initialVariant = this.entityVariants.get(0).id();
                }

                for (int i = 0; i < this.entityVariants.size(); i++) {
                    if (this.entityVariants.get(i).id().equals(this.initialVariant)) {
                        this.currentVariantIndex = i;
                        break;
                    }
                }
            }
            return;
        }

        Object renderEntry = EntryResolver.resolveCoreEntry(entry);

        if (isCobblemon(entry)) {
            ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
            this.renderedEntity = ClientFieldGuideCobblemonCompat.getDummyPokemon(id, this.minecraft.level);
        } else if (renderEntry instanceof EntityType<?> type) {
            try {
                this.renderedEntity = DummyEntities.create(type, this.minecraft.level);

                if (Services.PLATFORM.isModLoaded("mixed_litter")) {
                    Services.PLATFORM.applyMixedLitterCompat(this.renderedEntity);
                }
            } catch (Exception ignored) {
            }
        }

        if (this.renderedEntity != null) {
            this.entityVariants = FieldGuideVariantManager.getVariants(this.renderedEntity);
            if (this.entityVariants.size() > 1) {
                int centerX = leftPageBounds.x_center();
                int centerY = leftPageBounds.y_center() - 15;

                this.prevVariantButton = new PageTurnButton(centerX - 70, centerY - 8, 16, 16, ClientConstants.PREV_SPRITES, b -> cycleVariant(-1));
                this.nextVariantButton = new PageTurnButton(centerX + 54, centerY - 8, 16, 16, ClientConstants.NEXT_SPRITES, b -> cycleVariant(1));

                this.addRenderableWidget(prevVariantButton);
                this.addRenderableWidget(nextVariantButton);

                if (this.initialVariant == null) {
                    this.initialVariant = ProgressManager.getInstance().getSelectedVariant(entry);
                }

                VariantProvider<Mob> provider = FieldGuideVariantManager.getProvider(this.renderedEntity);
                if (provider != null) {
                    VariantDef current = provider.getCurrent((Mob) this.renderedEntity);
                    for (int i = 0; i < this.entityVariants.size(); i++) {
                        if (this.entityVariants.get(i).id().equals(this.initialVariant)) {
                            this.currentVariantIndex = i;
                            if (isCobblemon(entry) && this.minecraft != null) {
                                ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
                                this.renderedEntity = ClientFieldGuideCobblemonCompat.getDummyVariant(id, this.initialVariant, this.minecraft.level);
                            } else {
                                provider.apply((Mob) this.renderedEntity, this.entityVariants.get(i));
                            }
                            break;
                        } else if (this.initialVariant == null && this.entityVariants.get(i).id().equals(current.id())) {
                            this.currentVariantIndex = i;
                            this.initialVariant = this.entityVariants.get(i).id();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean onMouseClicked(ClickInput input) {
        double mouseX = input.x();
        double mouseY = input.y();
        int button = input.button();
        if (this.variantOverviewWidget != null && this.variantOverviewWidget.isVisible()) {
            if (input.sendTo(this.variantOverviewWidget)) return true;
        }

        if (this.friendMoonWidget != null && this.friendMoonWidget.visible && input.sendTo(this.friendMoonWidget)) return true;

        if (super.onMouseClicked(input)) return true;

        List<Season> seasons = SeasonsAPI.getGrowingSeasons(entry);
        if (!seasons.isEmpty()) {
            int iconSize = 8;
            int spacing = 2;
            int totalWidth = (iconSize * seasons.size()) + (spacing * (seasons.size() - 1));
            int xPos = leftPageBounds.x_center();
            int yPos = leftPageBounds.y_center() - 15;
            int startX = xPos - (totalWidth / 2);
            int startY = yPos + 42;

            for (int i = 0; i < seasons.size(); i++) {
                int drawX = startX + (i * (iconSize + spacing));
                if (Bounds.isMouseOver(mouseX, mouseY, drawX - 2, startY - 2, iconSize + 4, iconSize + 4)) {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new FieldGuideCategoryScreen("=$" + seasons.get(i).getId(), this));
                        return true;
                    }
                }
            }
        }

        Object clickEntry = EntryResolver.resolveCoreEntry(entry);

        if ((button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT) && (renderedEntity != null || clickEntry instanceof Block || clickEntry instanceof Item || this.variantOverviewWidget != null)) {
            int xPos = leftPageBounds.left() + leftPageBounds.width() / 2;
            int yPos = leftPageBounds.y_center() - 18;
            if (mouseX >= xPos - 50 && mouseX <= xPos + 50 && mouseY >= yPos - 50 && mouseY <= yPos + 50) {
                if (ClientFieldGuideManager.isUnlocked(entry)) {
                    if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                        if (this.variantOverviewWidget != null) {
                            this.variantOverviewWidget.toggleVisibility();
                        }
                        EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(entry);

                        if (visual != null && visual.customSound != null && this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(visual.customSound), 1.0F, 1.0F));
                        } else if ((isCobblemon(entry) || clickEntry instanceof EntityType<?>) && renderedEntity != null) {
                            FieldGuideClient.playMobCry(this.renderedEntity);
                        } else if (clickEntry instanceof Block block && this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(block.defaultBlockState().getSoundType().getBreakSound(), 1.0F, 1.0F));
                        } else if (clickEntry instanceof Item && this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Constants.ITEM_PICKUP_SOUND), 1.0F, 1.0F));
                        }
                    }
                    this.lastClickTime = System.currentTimeMillis();
                }
                return true;
            }
        }

        return false;
    }

    @Override
    //? if >=1.21 {
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    //?} else {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        double deltaX = 0;
    *///?}
        if (this.variantOverviewWidget != null && this.variantOverviewWidget.isVisible()) {
            //? if >=1.21 {
            this.variantOverviewWidget.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
            //?} else {
            /*this.variantOverviewWidget.mouseScrolled(mouseX, mouseY, deltaY);
            *///?}
            return true;
        }

        //? if >=1.21 {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        //?} else {
        /*if (super.mouseScrolled(mouseX, mouseY, deltaY)) return true;
        *///?}

        if (deltaY != 0 && !entityVariants.isEmpty() && this.leftPageBounds != null && this.leftPageBounds.contains((int) mouseX, (int) mouseY)) {
            int direction = deltaY > 0 ? -1 : 1;
            int nextIndex = currentVariantIndex + direction;

            if (nextIndex >= 0 && nextIndex < entityVariants.size()) {
                cycleVariant(direction);
                return true;
            }
        }

        return false;
    }

    private ResourceLocation getDetailsTexture() {
        if (!ClientFieldGuideManager.isUnlocked(entry)) {
            return Constants.DETAILS_PAGE_TEXTURE;
        }

        boolean hasVariants = !entityVariants.isEmpty();
        boolean hasAttributes = !activeAttributes.isEmpty();

        if (hasVariants && hasAttributes) {
            return Constants.DETAILS_PAGE_VA_TEXTURE;
        } else if (hasVariants) {
            return Constants.DETAILS_PAGE_V_TEXTURE;
        } else if (hasAttributes) {
            return Constants.DETAILS_PAGE_A_TEXTURE;
        } else {
            return Constants.DETAILS_PAGE_TEXTURE;
        }
    }

    @Override
    //? if <26.1 {
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderFieldGuideBackground(guiGraphics, mouseX, mouseY, partialTick);
    //?} else {
    /*public void extractRenderState(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractFieldGuideBackground(guiGraphics, mouseX, mouseY, partialTick);
    *///?}

        GuiCompat.push(guiGraphics);

        GuiCompat.blit(guiGraphics, Constants.BOOK_TEXTURE, this.bounds.left(), this.bounds.top(), 0, 0, this.bounds.width(), this.bounds.height(), this.bounds.width(), this.bounds.height());

        ResourceLocation detailsTexture = getDetailsTexture();
        GuiCompat.blit(guiGraphics, detailsTexture, this.bounds.left(), this.bounds.top(), 0, 0, this.bounds.width(), this.bounds.height(), this.bounds.width(), this.bounds.height());

        boolean unlocked = ClientFieldGuideManager.isUnlocked(entry);

        int titleY = this.leftPageBounds.top() + 8;
        int titleX = this.rightPageBounds.left() + 6;
        int textAreaWidth = this.rightPageBounds.width() - 10;

        if (!unlocked) {
            String currentVariantId = (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) ? entityVariants.get(currentVariantIndex).id() : null;
            String hintText = ClientTextManager.getInstance().getLockedHint(entry, currentVariantId);

            //? if <26.1 {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            //?}
            guiGraphics.drawString(this.font, GuiCompat.ellipsize(this.font, getTitleForEntry(entry).getString(), textAreaWidth), titleX, titleY, ClientConfig.get().getTextMutedColorInt(), false);
            GuiCompat.textWithWordWrap(guiGraphics, font, Component.literal(hintText), titleX, titleY + 21, textAreaWidth, ClientConfig.get().getTextMutedColorInt());
        } else {
            long discoveryTime = ProgressManager.getInstance().getDiscoveryTime(entry);
            if (showsUnlockQuill()) {
                Component dateComponent;

                if (ClientConfig.get().useRealWorldDate) {
                    String realDate = new SimpleDateFormat("MMM dd, yyyy")
                            .format(new Date(discoveryTime));

                    dateComponent = Component.literal(realDate);
                } else {
                    long gameTime = ProgressManager.getInstance().getDiscoveryGameTime(entry);
                    long days = gameTime / 24000L + 1;
                    String timeKey = getTimeKey(gameTime);

                    dateComponent = Component.translatable("fieldguide.date.in_game", days, Component.translatable(timeKey));
                }

                int quillX = this.rightPageBounds.right() - 15;
                int quillY = this.rightPageBounds.top() + 5;
                //? if <26.1 {
                RenderSystem.enableBlend();
                //?}
                GuiCompat.blit(guiGraphics, Constants.QUILL_ICON, quillX, quillY, 0, 0, 11, 11, 11, 11);
                //? if <26.1 {
                RenderSystem.disableBlend();
                //?}

                if (mouseX >= quillX && mouseX <= quillX + 11 && mouseY >= quillY && mouseY <= quillY + 11) {
                    guiGraphics.renderTooltip(this.font, dateComponent, mouseX, mouseY);
                }
            }

            if (ServerConfig.get().disableEditingNames) {
                guiGraphics.drawString(this.font, GuiCompat.ellipsize(this.font, ClientFieldGuideManager.getEntryName(entry).getString(), getNameWidth()), titleX, titleY, ClientConfig.get().getTextTitleColorInt(), false);
                if (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) {
                    VariantDef variant = entityVariants.get(currentVariantIndex);
                    String variantId = variant.id();
                    String customVariantName = ProgressManager.getInstance().getCustomName(ClientFieldGuideManager.getEntryId(entry).toString() + "#" + variantId);

                    Component variantName;
                    if (variant.value() instanceof com.evandev.fieldguide.api.EntryVariantData vd && vd.displayName() != null) {
                        variantName = vd.displayName();
                    } else {
                        variantName = customVariantName != null ? Component.literal(customVariantName) : FieldGuideVariantManager.getVariantDisplayName(variant);
                    }

                    guiGraphics.drawString(this.font, GuiCompat.ellipsize(this.font, variantName.getString(), textAreaWidth), titleX, titleY + LINE_HEIGHT + 3, ClientConfig.get().getTextMutedColorInt(), false);
                }
            }

            renderDynamicAttributes(guiGraphics, mouseX, mouseY);

            if (ServerConfig.get().disableEditingDescriptions) {
                int textY = titleY + LINE_HEIGHT + 3; // Name
                if (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) {
                    textY += LINE_HEIGHT; // Variant
                }
                if (!activeAttributes.isEmpty()) {
                    textY += (LINE_HEIGHT * 2); // Attributes
                } else {
                    textY += LINE_HEIGHT; // Buffer
                }
                this.descX = titleX;
                this.descY = textY;
                this.descW = textAreaWidth;
                if (!NoMansLandCompat.isReplayActive()) {
                    GuiCompat.textWithWordWrap(guiGraphics, font, Component.literal(ClientFieldGuideManager.getEntryDescription(entry)), titleX, textY, textAreaWidth, ClientConfig.get().getTextColorInt());
                }
            }
        }

        float bounce = 1.0f;
        long elapsed = System.currentTimeMillis() - lastClickTime;
        if (elapsed < 150) bounce = 1.0f - 0.05f * (float) Math.sin((elapsed / 150.0f) * Math.PI);

        int xPos = leftPageBounds.x_center();
        int yPos = leftPageBounds.y_center() - 15;

        String variantId = (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) ? entityVariants.get(currentVariantIndex).id() : null;
        boolean hideEntity = Services.PLATFORM.isModLoaded("exposure") && ClientExposureCompat.hasPhotograph(entry, variantId);

        boolean mouseOverEntity = mouseX >= xPos - 50 && mouseX <= xPos + 50 && mouseY >= yPos - 50 && mouseY <= yPos + 50;

        float targetScale = 1.0f;
        if (!hideEntity && mouseOverEntity && ClientFieldGuideManager.isUnlocked(entry) && this.variantOverviewWidget != null && !this.variantOverviewWidget.isVisible()) {
            boolean currentVariantUnlocked = ServerConfig.get().unlockAllVariants || entityVariants.isEmpty() || isVisualVariantUnlocked(entry, entityVariants.get(currentVariantIndex));
            if (currentVariantUnlocked) {
                targetScale = 1.05f;
            }
        }

        long currentTime = System.currentTimeMillis();
        if (lastRenderTime > 0) {
            float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
            float speed = 10.0f;
            float factor = 1.0f - (float) Math.pow(0.01, deltaTime * speed);
            hoverScale = hoverScale + (targetScale - hoverScale) * factor;
        }
        lastRenderTime = currentTime;

        bounce *= hoverScale;

        Object renderEntry = EntryResolver.resolveCoreEntry(entry);

        GuiCompat.push(guiGraphics);
        GuiCompat.translate(guiGraphics, xPos, yPos);
        GuiCompat.scale(guiGraphics, bounce);
        GuiCompat.translate(guiGraphics, -xPos, -yPos);

        if (entry instanceof GuideEntry ge && ge.visualVariants() != null && !ge.visualVariants().isEmpty() && currentVariantIndex < entityVariants.size()) {
            if (!hideEntity) {
                EntryVariantData currentVarData = ge.visualVariants().get(currentVariantIndex);
                boolean variantUnlocked = isVisualVariantUnlocked(entry, entityVariants.get(currentVariantIndex));
                Entity variantEntity = null;
                if (currentVarData.displayType() == EntryVariantData.DisplayType.ENTITY && this.minecraft != null) {
                    variantEntity = variantEntityCache.computeIfAbsent(currentVarData.variantId(),
                            k -> EntryRenderHelper.createVariantEntity(this.minecraft.level, currentVarData.displayId(), currentVarData.nbt()));
                }
                EntryRenderHelper.renderVisualVariant(guiGraphics, ge, currentVarData, variantEntity, xPos, yPos, 112, variantUnlocked, true, 1.0f);
            }
        } else if (entry instanceof GuideEntry ge && ge.isStructure() && renderEntry instanceof Block block) {
            if (!hideEntity) {
                if (ge.structureData() != null && (ge.structureData().structureNbt() != null || (ge.structureData().stackedBlocks() != null && !ge.structureData().stackedBlocks().isEmpty()))) {
                    EntryRenderHelper.renderStructure(guiGraphics, ge, xPos, yPos, 112, unlocked, true, 1.0f);
                } else {
                    EntryRenderHelper.renderBlock(guiGraphics, block, xPos, yPos, 40.0F, unlocked, true, 1.0f);
                }
            }
        } else if (entry instanceof GuideEntry ge && FieldGuideCobblemonCompat.isCobblemonEntry(ge)) {
            if (!hideEntity) {
                EntryRenderHelper.renderCobblemon(guiGraphics, ge, xPos, yPos, 112, 112, unlocked, true, 1.0f);
            }
        } else if (entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && ge.virtualData().virtualType().equals("tutorial")) {
            if (!hideEntity) {
                EntryRenderHelper.renderTutorial(guiGraphics, ge, xPos, yPos, 112, 112, unlocked, true, 1.0f);
            }
        } else if (renderEntry instanceof EntityType && renderedEntity != null) {
            boolean variantUnlocked = unlocked;
            if (unlocked && !entityVariants.isEmpty() && !ServerConfig.get().unlockAllVariants) {
                variantUnlocked = ClientFieldGuideManager.isVariantUnlocked(entry, entityVariants.get(currentVariantIndex).id());
            }

            if (!hideEntity) {
                EntryRenderHelper.renderEntityNormalized(guiGraphics, renderedEntity, xPos, yPos, 112, 112, variantUnlocked, true, 1.0f);
            }
        } else if (renderEntry instanceof Block block) {
            if (!hideEntity) {
                EntryRenderHelper.renderBlock(guiGraphics, block, xPos, yPos, 40.0F, unlocked, true, 1.0f);
            }
        } else if (renderEntry instanceof Item item) {
            if (!hideEntity) {
                EntryRenderHelper.renderItem(guiGraphics, item, xPos, yPos, 60.0F, unlocked, true, 1.0f);
            }
        }
        GuiCompat.pop(guiGraphics);

        if (unlocked) {
            renderSeasons(guiGraphics, xPos, yPos, mouseX, mouseY);
        }

        if (!dataLoaded) {
            guiGraphics.drawString(this.font, Component.translatable("gui.fieldguide.loading"), this.bounds.left() + 20, this.bounds.bottom() - 30, ClientConfig.get().getTextMutedColorInt(), false);
        }

        GuiCompat.pop(guiGraphics);

        boolean replaying = ClientFieldGuideManager.isUnlocked(entry) && NoMansLandCompat.isReplayActive();
        if (this.descriptionWidget != null) this.descriptionWidget.visible = !replaying;
        //? if <26.1 {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        //?} else {
        /*super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        *///?}
        if (replaying) {
            NoMansLandCompat.renderDescriptionRewrite(guiGraphics, this.font, descX, descY, descW, ClientConfig.get().getTextColorInt());
        }
    }

    private void renderSeasons(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        List<Season> seasons = SeasonsAPI.getGrowingSeasons(entry);
        if (seasons.isEmpty()) return;

        int iconSize = 8;
        int spacing = 2;
        int totalWidth = (iconSize * seasons.size()) + (spacing * (seasons.size() - 1));
        int startX = x - (totalWidth / 2);
        int startY = y + 42;

        Season hoveredSeason = null;

        for (int i = 0; i < seasons.size(); i++) {
            Season season = seasons.get(i);
            int drawX = startX + (i * (iconSize + spacing));

            boolean hovered = Bounds.isMouseOver(mouseX, mouseY, drawX - 2, startY - 2, iconSize + 4, iconSize + 4);

            //? if <26.1 {
            RenderSystem.enableBlend();
            //?}
            if (hovered) {
                GuiCompat.push(guiGraphics);
                GuiCompat.translate(guiGraphics, drawX + iconSize / 2.0, startY + iconSize / 2.0);
                GuiCompat.scale(guiGraphics, 1.1f);
                GuiCompat.translate(guiGraphics, -(drawX + iconSize / 2.0), -(startY + iconSize / 2.0));
            }

            int u = 0;
            int v = 0;
            switch (season) {
                case SUMMER -> u = 8;
                case AUTUMN -> v = 8;
                case WINTER -> {
                    u = 8;
                    v = 8;
                }
            }
            GuiCompat.blit(guiGraphics, Constants.SEASONS_TEXTURE, drawX, startY, u, v, iconSize, iconSize, 8, 8, 16, 16);

            if (hovered) {
                GuiCompat.pop(guiGraphics);
            }
            //? if <26.1 {
            RenderSystem.disableBlend();
            //?}

            if (hovered) {
                hoveredSeason = season;
            }
        }

        if (hoveredSeason != null) {
            guiGraphics.renderTooltip(this.font, hoveredSeason.getDisplayName(), mouseX, mouseY);
        }
    }

    private void cycleVariant(int dir) {
        if (entityVariants.isEmpty()) return;
        currentVariantIndex = (currentVariantIndex + dir + entityVariants.size()) % entityVariants.size();
        this.initialVariant = entityVariants.get(currentVariantIndex).id();

        if (ServerConfig.get().unlockAllVariants || isVisualVariantUnlocked(entry, entityVariants.get(currentVariantIndex))) {
            ProgressManager.getInstance().setSelectedVariant(entry, this.initialVariant);
        }

        this.dataLoaded = false;
        this.isLoadingData = false;
        this.refresh();
        this.lastClickTime = System.currentTimeMillis();
    }

    private void setVariantIndex(int index) {
        if (entityVariants.isEmpty()) return;

        if (index >= 0 && index < entityVariants.size()) {
            currentVariantIndex = index;
            this.initialVariant = entityVariants.get(currentVariantIndex).id();

            if (ServerConfig.get().unlockAllVariants || ClientFieldGuideManager.isVariantUnlocked(entry, this.initialVariant)) {
                ProgressManager.getInstance().setSelectedVariant(entry, this.initialVariant);
            }

            this.dataLoaded = false;
            this.isLoadingData = false;
            this.refresh();
            this.lastClickTime = System.currentTimeMillis();
        }
    }

    private void loadSpawnBiomes() {
        spawnBiomes.clear();
        spawnBiomes.addAll(ClientFieldGuideManager.getInstance().getResolvedBiomes(entry, this.initialVariant));
    }

    private void setupBiomeWidget(boolean unlocked) {
        if (ServerConfig.get().disableBiomeDisplay || !Services.PLATFORM.isModLoaded("immersiveoverlays")) return;

        if (unlocked && !spawnBiomes.isEmpty()) {
            this.addRenderableWidget(EntryBiomeWidgetHelper.createBiomeGridWidget(
                    this.rightPageBounds,
                    this.spawnBiomes,
                    this.font,
                    () -> this.variantOverviewWidget,
                    this
            ));
        }
    }

    private void setupDropWidget(boolean unlocked) {
        if (ServerConfig.get().disableLootDisplay) return;
        List<ItemStack> drops = loadedDrops;

        if (!drops.isEmpty()) {
            int dropItemSize = 20;
            this.addRenderableWidget(new PaginatedGridWidget<>(this.leftPageBounds.left() + 2, this.leftPageBounds.bottom() - 33, this.leftPageBounds.width() - 4, dropItemSize, 5, dropItemSize, 0, drops, (graphics, stack, x, y, mouseX, mouseY) -> {
                //? if <26.1 {
                RenderSystem.enableDepthTest();
                //?}
                boolean mouseOver = Bounds.isMouseOver(mouseX, mouseY, x, y, dropItemSize, dropItemSize) && (this.variantOverviewWidget == null || !this.variantOverviewWidget.isMouseOver(mouseX, mouseY));
                int backgroundOffset = mouseOver ? dropItemSize : 0;
                GuiCompat.blit(graphics, Constants.WIDGETS_TEXTURE, x, y, 0, 64 + backgroundOffset, dropItemSize, dropItemSize, 256, 256);
                int offset = (dropItemSize - 16) / 2;
                //? if <26.1 {
                graphics.renderItem(stack, x + offset, y + offset);
                graphics.renderItemDecorations(this.font, stack, x + offset, y + offset, "");
                //?} else {
                /*graphics.fakeItem(stack, x + offset, y + offset);
                graphics.itemDecorations(this.font, stack, x + offset, y + offset, "");
                *///?}
                if (mouseOver) {
                    Minecraft mc = Minecraft.getInstance();
                    List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(mc, stack));
                    if (ModDataComponents.has(stack, ModDataComponents.DROP_CHANCE)) {
                        float dropChance = ModDataComponents.get(stack, ModDataComponents.DROP_CHANCE);
                        tooltip.add(Component.literal(String.format(Locale.ROOT, "%.2f%%", dropChance)).withStyle(ChatFormatting.GRAY));
                    }
                    //? if <26.1 {
                    graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    //?} else {
                    /*graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                    *///?}
                }
            }, stack -> {
                if (this.minecraft != null)
                    this.minecraft.setScreen(new FieldGuideCategoryScreen("=^" + stack.getHoverName().getString().toLowerCase(Locale.ROOT), this));
            }));
        }
    }

    private void setupNavigationButtons() {
        this.addRenderableWidget(new PageTurnButton(this.bounds.right() - 13, this.bounds.top() + 26, 24, 24, ClientConstants.BACK_SPRITES, b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        })).setTooltip(Tooltip.create(Component.translatable("gui.fieldguide.back")));

        List<Object> entries = parent.getCurrentEntries();

        if (!entries.isEmpty()) {
            int index = entries.indexOf(this.entry);

            PageTurnButton prevEntryButton = new PageTurnButton(this.bounds.left() + 15, this.leftPageBounds.bottom() - 15, 16, 16, ClientConstants.PREV_PAGE_SPRITES, b -> {
                if (index > 0 && this.minecraft != null)
                    this.minecraft.setScreen(new FieldGuideEntryScreen(parent, entries.get(index - 1)));
            });
            PageTurnButton nextEntryButton = new PageTurnButton(this.bounds.right() - 30, this.rightPageBounds.bottom() - 15, 16, 16, ClientConstants.NEXT_PAGE_SPRITES, b -> {
                if (index >= 0 && index < entries.size() - 1 && this.minecraft != null)
                    this.minecraft.setScreen(new FieldGuideEntryScreen(parent, entries.get(index + 1)));
            });

            prevEntryButton.visible = index > 0;
            nextEntryButton.visible = index >= 0 && index < entries.size() - 1;
            this.addRenderableWidget(prevEntryButton);
            this.addRenderableWidget(nextEntryButton);
        }

        this.addRenderableWidget(new FieldGuideSearchBox(this.font, this.width / 2 - 70, this.bounds.bottom() + 5, 140, 20, "", q -> {
            if (!q.isEmpty() && this.minecraft != null) {
                FieldGuideCategoryScreen searchScreen = new FieldGuideCategoryScreen(q, this);
                searchScreen.setInitialSearchFocus(true);
                this.minecraft.setScreen(searchScreen);
            }
        }));
    }

    @Override
    protected boolean onKeyPressed(KeyInput input) {
        if (this.getFocused() instanceof AbstractWidget widget && widget.isFocused()) {
            if (input.matches(this.minecraft.options.keyInventory)) return true;
            if (input.matches(FieldGuideClient.OPEN_GUIDE_KEY)) return true;
        }

        if (this.minecraft.player != null && input.matches(this.minecraft.options.keyInventory)) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            return true;
        }

        return super.onKeyPressed(input);
    }

    @Override
    public void onTabClick(Category category) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
            parent.onTabClick(category);
        }
    }

    private void renderDynamicAttributes(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (activeAttributes.isEmpty()) return;

        int titleHeight = 24;
        int titleWithVariantHeight = 35;

        int currentY = this.leftPageBounds.top();
        if (!entityVariants.isEmpty() && currentVariantIndex < entityVariants.size()) {
            currentY += titleWithVariantHeight;
        } else {
            currentY += titleHeight;
        }

        int drawX = this.rightPageBounds.left() + 6 - 4;
        //? if <26.1 {
        EntryAttributesRenderer.renderAttributes(guiGraphics, this.font, activeAttributes, currentY, drawX, mouseX, mouseY, this::setTooltipForNextRenderPass);
        //?} else {
        /*EntryAttributesRenderer.renderAttributes(guiGraphics, this.font, activeAttributes, currentY, drawX, mouseX, mouseY, tooltip -> guiGraphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY));
        *///?}
    }

    private Tooltip createCopyTooltip(boolean canCopy) {
        MutableComponent tooltip = Component.translatable("gui.fieldguide.copy.tooltip");
        if (!canCopy) {
            tooltip.append(CommonComponents.NEW_LINE).append(Component.translatable("gui.fieldguide.copy.requires_paper").withStyle(ChatFormatting.RED));
        }
        return Tooltip.create(tooltip);
    }
}

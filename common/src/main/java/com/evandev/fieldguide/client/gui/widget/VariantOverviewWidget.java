package com.evandev.fieldguide.client.gui.widget;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientConstants;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.util.Bounds;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.exposure.ClientExposureCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class VariantOverviewWidget extends AbstractWidget {

    private final Object entry;
    private final LivingEntity originalRenderedEntity;
    private final List<VariantDef> variants;
    private final Consumer<Integer> onVariantSelected;
    private final Runnable onToggle;
    private final int maxPages;
    private final PageTurnButton leftButton;
    private final PageTurnButton rightButton;
    private final float[] hoverScales = new float[9];
    private final Map<String, Entity> variantEntityCache = new HashMap<>();
    private int currentPage = 0;
    private long lastRenderTime = 0;

    public VariantOverviewWidget(int x, int y, int width, int height, Object entry, LivingEntity renderedEntity, List<VariantDef> variants, Consumer<Integer> onVariantSelected, Runnable onToggle) {
        super(x, y, width, height, Component.empty());
        this.entry = entry;
        this.originalRenderedEntity = renderedEntity;
        this.variants = variants;
        this.onVariantSelected = onVariantSelected;
        this.onToggle = onToggle;
        this.maxPages = (int) Math.ceil(variants.size() / 9.0);
        this.visible = false;

        Arrays.fill(hoverScales, 1.0f);

        this.leftButton = new PageTurnButton(x + (width / 2) - 24, y + height - 20, 16, 16, ClientConstants.PREV_SPRITES, (btn) -> {
            if (currentPage > 0) {
                currentPage--;
                Arrays.fill(hoverScales, 1.0f);
            }
        });
        this.rightButton = new PageTurnButton(x + (width / 2) + 8, y + height - 20, 16, 16, ClientConstants.NEXT_SPRITES, (btn) -> {
            if (currentPage < maxPages - 1) {
                currentPage++;
                Arrays.fill(hoverScales, 1.0f);
            }
        });

        preGenerateEntities();
    }

    private void preGenerateEntities() {
        if (Minecraft.getInstance().level == null) return;

        boolean hasEntryVariantData = variants.stream().anyMatch(v -> v.value() instanceof EntryVariantData);
        if (hasEntryVariantData) {
            for (VariantDef variant : variants) {
                if (variant.value() instanceof EntryVariantData vd && vd.displayType() == EntryVariantData.DisplayType.ENTITY) {
                    Entity entity = EntryRenderHelper.createVariantEntity(Minecraft.getInstance().level, vd.displayId(), vd.nbt());
                    if (entity != null) variantEntityCache.put(variant.id(), entity);
                }
            }
            return;
        }

        if (originalRenderedEntity == null) return;

        VariantProvider<Mob> provider = (originalRenderedEntity instanceof Mob mob) ? FieldGuideVariantManager.getProvider(mob) : null;

        CompoundTag originalTag = new CompoundTag();
        originalRenderedEntity.saveWithoutId(originalTag);

        for (VariantDef variant : variants) {
            if (Services.PLATFORM.isModLoaded("cobblemon") && FieldGuideCobblemonCompat.isPokemon(originalRenderedEntity)) {
                ResourceLocation id = ClientFieldGuideManager.getEntryId(entry);
                if (id != null) {
                    variantEntityCache.put(variant.id(), ClientFieldGuideCobblemonCompat.getDummyVariant(id, variant.id(), Minecraft.getInstance().level));
                }
            } else {
                Entity newEnt = originalRenderedEntity.getType().create(Minecraft.getInstance().level);
                if (newEnt instanceof LivingEntity freshLiving) {

                    UUID uuid = freshLiving.getUUID();
                    try {
                        freshLiving.load(originalTag);
                    } catch (Exception e) {
                        Constants.LOG.warn("Failed to load entity data for variant display ({}): {}", freshLiving.getType().getDescriptionId(), e.getMessage());
                    }
                    freshLiving.setUUID(uuid);

                    if (originalRenderedEntity instanceof AgeableMob origAgeable && freshLiving instanceof AgeableMob freshAgeable) {
                        freshAgeable.setAge(origAgeable.getAge());
                    }

                    if (provider != null && freshLiving instanceof Mob freshMob) {
                        provider.apply(freshMob, variant);
                    }
                    variantEntityCache.put(variant.id(), freshLiving);
                }
            }
        }
    }

    public void toggleVisibility() {
        this.visible = !this.visible;
        if (this.onToggle != null) this.onToggle.run();
    }

    public boolean isVisible() {
        return this.visible;
    }

    private boolean checkUnlocked(VariantDef variant) {
        if (ServerConfig.get().unlockAllVariants) return true;

        return ClientFieldGuideManager.isVariantUnlocked(this.entry, variant.id());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) return false;

        if (scrollY > 0 && this.currentPage > 0) {
            this.currentPage--;
            return true;
        } else if (scrollY < 0 && this.currentPage < maxPages - 1) {
            this.currentPage++;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);

        graphics.blit(Constants.VARIANT_WIDGET_TEXTURE, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        Component tooltipText = null;

        int startIdx = currentPage * 9;
        int endIdx = Math.min(startIdx + 9, variants.size());

        long currentTime = System.currentTimeMillis();
        float deltaTime = lastRenderTime > 0 ? (currentTime - lastRenderTime) / 1000.0f : 0.0f;
        lastRenderTime = currentTime;

        for (int i = startIdx; i < endIdx; i++) {
            int gridIndex = i - startIdx;
            Bounds bounds = getGridCellBoundsLocal(gridIndex);
            boolean hovered = bounds.contains(mouseX, mouseY);
            VariantDef variant = variants.get(i);

            boolean isUnlocked = checkUnlocked(variant);

            Entity renderEntity = variantEntityCache.getOrDefault(variant.id(), originalRenderedEntity);

            graphics.pose().pushPose();

            int centerX = bounds.x_center();
            int centerY = bounds.y_center();
            graphics.pose().translate(centerX, centerY, 0);
            float currentScale = hoverScales[gridIndex];
            graphics.pose().scale(currentScale, currentScale, currentScale);

            graphics.pose().translate(-centerX, -centerY, 0);

            boolean renderedPhoto = false;
            if (isUnlocked && Services.PLATFORM.isModLoaded("exposure") && ClientConfig.get().exposureShowPhotographsInGrid) {
                ItemStack existingPhoto = ProgressManager.getInstance().getPhotograph(entry, variant.id());
                if (!existingPhoto.isEmpty()) {
                    ClientExposureCompat.renderPhotographInGrid(graphics, centerX - (bounds.width() / 2), centerY - (bounds.height() / 2), bounds.width(), bounds.height(), existingPhoto);
                    renderedPhoto = true;
                } else if (ServerConfig.get().keepSilhouetteWhenUnlocked) {
                    ClientExposureCompat.renderMissingPhotoBackground(graphics, centerX - (bounds.width() / 2), centerY - (bounds.height() / 2), bounds.width(), bounds.height());
                }
            }

            if (!renderedPhoto) {
                if (variant.value() instanceof EntryVariantData vd && entry instanceof GuideEntry ge) {
                    EntryRenderHelper.renderVisualVariant(graphics, ge, vd, renderEntity, centerX, centerY, bounds.width(), isUnlocked, false, 1.0f);
                } else if (renderEntity != null) {
                    EntryRenderHelper.renderEntityNormalized(graphics, renderEntity, centerX, centerY, bounds.width(), bounds.height(), isUnlocked, false, 1.0f, variant.id());
                }
            }

            float targetScale = (hovered && isUnlocked) ? 1.05f : 1.0f;
            if (deltaTime > 0) {
                float speed = 10.0f;
                float factor = 1.0f - (float) Math.pow(0.01, deltaTime * speed);
                hoverScales[gridIndex] = hoverScales[gridIndex] + (targetScale - hoverScales[gridIndex]) * factor;
            } else {
                hoverScales[gridIndex] = targetScale;
            }

            graphics.pose().popPose();

            if (hovered) {
                if (isUnlocked) {
                    String customVariantName = ProgressManager.getInstance().getCustomName(ClientFieldGuideManager.getEntryId(entry).toString() + "#" + variant.id());

                    if (variant.value() instanceof EntryVariantData vd && vd.displayName() != null) {
                        tooltipText = customVariantName != null ? Component.literal(customVariantName) : vd.displayName();
                    } else {
                        tooltipText = customVariantName != null ? Component.literal(customVariantName) : FieldGuideVariantManager.getVariantDisplayName(variant);
                    }
                } else {
                    tooltipText = Component.literal("???");
                }
            }
        }

        if (tooltipText != null) {
            graphics.renderTooltip(Minecraft.getInstance().font, tooltipText, mouseX, mouseY);
        }

        if (this.currentPage > 0) this.leftButton.render(graphics, mouseX, mouseY, partialTicks);
        if (this.currentPage < maxPages - 1) this.rightButton.render(graphics, mouseX, mouseY, partialTicks);

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        if (this.currentPage > 0 && this.leftButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.currentPage < maxPages - 1 && this.rightButton.mouseClicked(mouseX, mouseY, button)) return true;

        int startIdx = currentPage * 9;
        int endIdx = Math.min(startIdx + 9, variants.size());

        for (int i = startIdx; i < endIdx; i++) {
            int gridIndex = i - startIdx;
            Bounds bounds = getGridCellBoundsLocal(gridIndex);

            if (bounds.contains((int) mouseX, (int) mouseY)) {
                VariantDef variant = variants.get(i);

                if (checkUnlocked(variant)) {
                    this.visible = false;
                    this.onVariantSelected.accept(i);
                    if (this.onToggle != null) this.onToggle.run();
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                }
                return true;
            }
        }

        if (mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    private Bounds getGridCellBoundsLocal(int i) {
        int cell_size = 40;
        int gap = 1;
        int startX = this.getX() + 6;
        int startY = this.getY() + 6;
        int localIndex = i % 9;
        int col = localIndex % 3;
        int row = localIndex / 3;
        int x = startX + (col * (cell_size + gap));
        int y = startY + (row * (cell_size + gap));
        return new Bounds(x, y, cell_size, cell_size);
    }
}
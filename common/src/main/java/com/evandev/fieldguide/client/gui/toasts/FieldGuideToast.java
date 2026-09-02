package com.evandev.fieldguide.client.gui.toasts;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.util.DummyEntities;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

public class FieldGuideToast implements Toast {
    private final Object entry;
    private final String variantId;
    private Entity cachedEntity = null;
    private boolean entityInitialized = false;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public FieldGuideToast(Object entry, String variantId) {
        this.entry = entry;
        this.variantId = variantId;
    }

    @Override
    public Toast.@NonNull Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        this.wantedVisibility = fullyVisibleForMs >= 5000L * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, @NonNull Font font, long fullyVisibleForMs) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.TOAST_TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 160, 32);

        Component name = ClientFieldGuideManager.getEntryName(entry);
        Component discovered = Component.translatable("fieldguide.toast.discovered");

        guiGraphics.text(font, name, 30, 7, ClientConfig.get().getTextTitleColorInt(), false);
        guiGraphics.text(font, discovered, 30, 17, 0xFFAF8C5C, false);

        int iconX = 16;
        int iconY = 17;
        Object coreEntry = EntryResolver.resolveCoreEntry(this.entry);

        boolean isTutorial = this.entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "tutorial".equals(ge.virtualData().virtualType());

        if (!entityInitialized) {
            if (coreEntry instanceof EntityType<?> type) {
                cachedEntity = DummyEntities.create(type, Objects.requireNonNull(Minecraft.getInstance().level), EntitySpawnReason.TRIGGERED);

                if (variantId != null && cachedEntity instanceof Mob mob) {
                    VariantProvider<Mob> provider = FieldGuideVariantManager.getProvider(mob);
                    if (provider != null) {
                        List<VariantDef> variants = FieldGuideVariantManager.getVariants(mob);
                        for (VariantDef def : variants) {
                            if (def.id().equals(variantId)) {
                                provider.apply(mob, def);
                                break;
                            }
                        }
                    }
                }
            }
            entityInitialized = true;
        }

        if (this.entry instanceof GuideEntry ge && ge.isStructure() && coreEntry instanceof Block) {
            EntryRenderHelper.renderStructure(guiGraphics, ge, iconX, iconY, 24, true, false, 1.0F);
        } else if (isTutorial) {
            EntryRenderHelper.renderTutorial(guiGraphics, (GuideEntry) this.entry, iconX, iconY, 24, 24, true, false, 1.0F);
        } else if (coreEntry instanceof EntityType<?>) {
            if (cachedEntity != null) {
                EntryRenderHelper.renderEntityNormalized(guiGraphics, cachedEntity, iconX, iconY, 24, 24, true, false, 1.0F, false);
            } else {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
            }
        } else if (coreEntry instanceof Block block) {
            EntryRenderHelper.renderBlock(guiGraphics, block, iconX, iconY, 12.0F, true, false, 1.0F);
        } else if (coreEntry instanceof Item item) {
            EntryRenderHelper.renderItem(guiGraphics, item, iconX, iconY, 20.0F, true, false, 1.0F);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
        }
    }
}
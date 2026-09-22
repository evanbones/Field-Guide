package com.evandev.fieldguide.client.gui.toasts;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FieldGuideToast implements Toast {
    private final Object entry;
    private final String variantId;
    private Entity cachedEntity = null;
    private boolean entityInitialized = false;

    public FieldGuideToast(Object entry, String variantId) {
        this.entry = entry;
        this.variantId = variantId;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics guiGraphics, @NotNull ToastComponent toastComponent, long timeSinceLastVisible) {
        guiGraphics.blit(Constants.TOAST_TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 160, 32);

        Component name = (this.variantId != null && !this.variantId.isEmpty())
                ? ClientFieldGuideManager.getEntryName(entry, this.variantId)
                : ClientFieldGuideManager.getEntryName(entry);
        Component discovered = Component.translatable("fieldguide.toast.discovered");

        guiGraphics.drawString(toastComponent.getMinecraft().font, name, 30, 7, ClientConfig.get().getTextTitleColorInt(), false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, discovered, 30, 17, 0xAF8C5C, false);

        int iconX = 16;
        int iconY = 17;
        Object coreEntry = EntryResolver.resolveCoreEntry(this.entry);

        boolean isCobblemon = FieldGuideCobblemonCompat.isCobblemonEntry(this.entry);
        boolean isTutorial = this.entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "tutorial".equals(ge.virtualData().virtualType());

        EntryVariantData visualVariant = null;
        if (this.entry instanceof GuideEntry ge && ge.hasVisualVariants()) {
            if (this.variantId != null && !this.variantId.isEmpty()) {
                for (EntryVariantData vd : ge.visualVariants()) {
                    if (vd.variantId().equals(this.variantId)) {
                        visualVariant = vd;
                        break;
                    }
                }
            }
            if (visualVariant == null) {
                String selected = ProgressManager.getInstance().getSelectedVariant(ge.id());
                if (selected != null) {
                    for (EntryVariantData vd : ge.visualVariants()) {
                        if (vd.variantId().equals(selected)) {
                            visualVariant = vd;
                            break;
                        }
                    }
                }
            }
            if (visualVariant == null && !ge.visualVariants().isEmpty()) {
                visualVariant = ge.visualVariants().getFirst();
            }
        }

        if (!entityInitialized) {
            if (visualVariant != null && visualVariant.displayType() == EntryVariantData.DisplayType.ENTITY) {
                if (Minecraft.getInstance().level != null) {
                    cachedEntity = EntryRenderHelper.createVariantEntity(Minecraft.getInstance().level, visualVariant.displayId(), visualVariant.nbt());
                }
            } else if (isCobblemon) {
                ResourceLocation id = ((GuideEntry) this.entry).id();
                if (variantId != null) {
                    cachedEntity = ClientFieldGuideCobblemonCompat.getDummyVariant(id, variantId, Minecraft.getInstance().level);
                } else {
                    cachedEntity = ClientFieldGuideCobblemonCompat.getDummyPokemon(id, Minecraft.getInstance().level);
                }
            } else if (coreEntry instanceof EntityType<?> type) {
                cachedEntity = type.create(Minecraft.getInstance().level);

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

        if (this.entry instanceof GuideEntry ge && ge.hasVisualVariants() && visualVariant != null) {
            EntryRenderHelper.renderVisualVariant(guiGraphics, ge, visualVariant, cachedEntity, iconX, iconY, 24, true, false, 1.0F);
        } else if (this.entry instanceof GuideEntry ge && ge.isStructure() && ge.structureData() != null && coreEntry instanceof Block) {
            EntryRenderHelper.renderStructure(guiGraphics, ge, iconX, iconY, 24, true, false, 1.0F);
        } else if (isCobblemon && cachedEntity instanceof LivingEntity) {
            EntryRenderHelper.renderCobblemon(guiGraphics, (GuideEntry) this.entry, iconX, iconY, 24, 24, true, false, 1.0F, false);
        } else if (isTutorial) {
            EntryRenderHelper.renderTutorial(guiGraphics, (GuideEntry) this.entry, iconX, iconY, 24, 24, true, false, 1.0F);
        } else if (coreEntry instanceof EntityType<?>) {
            if (cachedEntity != null) {
                EntryRenderHelper.renderEntityNormalized(guiGraphics, cachedEntity, iconX, iconY, 24, 24, true, false, 1.0F, false);
            } else {
                guiGraphics.blit(Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
            }
        } else if (coreEntry instanceof Block block) {
            EntryRenderHelper.renderBlock(guiGraphics, block, iconX, iconY, 12.0F, true, false, 1.0F);
        } else if (coreEntry instanceof Item item) {
            EntryRenderHelper.renderItem(guiGraphics, item, iconX, iconY, 20.0F, true, false, 1.0F);
        } else {
            guiGraphics.blit(Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
        }

        return timeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
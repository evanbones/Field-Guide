package com.evandev.fieldguide.client.gui.toasts;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.gui.util.EntryRenderHelper;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.util.DummyEntities;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

//? if <26.1 {
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.jetbrains.annotations.NotNull;
//?}

//? if >=26.1 {
/*import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.NonNull;

*///?}

public class FieldGuideToast implements Toast {
    private final Object entry;
    private final String variantId;
    private Entity cachedEntity = null;
    private EntryVariantData visualVariant = null;
    private String cobblemonForm = null;
    private boolean entityInitialized = false;
    //? if >=26.1 {
    /*private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    *///?}

    public FieldGuideToast(Object entry, String variantId) {
        this.entry = entry;
        this.variantId = variantId;
    }

    @Override
    //? if <26.1 {
    public @NotNull Visibility render(GuiGraphics guiGraphics, @NotNull ToastComponent toastComponent, long timeSinceLastVisible) {
        guiGraphics.blit(Constants.TOAST_TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 160, 32);
    //?} else {
    /*public Toast.@NonNull Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        this.wantedVisibility = fullyVisibleForMs >= 5000L * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphics guiGraphics, @NonNull Font font, long fullyVisibleForMs) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Constants.TOAST_TEXTURE, 0, 0, 0, 0, this.width(), this.height(), 160, 32);
    *///?}

        Component name = (this.variantId != null && !this.variantId.isEmpty())
                ? ClientFieldGuideManager.getEntryName(entry, this.variantId)
                : ClientFieldGuideManager.getEntryName(entry);
        Component discovered = Component.translatable("fieldguide.toast.discovered");

        //? if <26.1 {
        guiGraphics.drawString(toastComponent.getMinecraft().font, GuiCompat.ellipsize(toastComponent.getMinecraft().font, name.getString(), this.width() - 34), 30, 7, ClientConfig.get().getTextTitleColorInt(), false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, discovered, 30, 17, 0xAF8C5C, false);
        //?} else {
        /*guiGraphics.text(font, Component.literal(GuiCompat.ellipsize(font, name.getString(), this.width() - 34)), 30, 7, ClientConfig.get().getTextTitleColorInt(), false);
        guiGraphics.text(font, discovered, 30, 17, 0xFFAF8C5C, false);
        *///?}

        int iconX = 16;
        int iconY = 17;
        Object coreEntry = EntryResolver.resolveCoreEntry(this.entry);

        boolean isCobblemon = FieldGuideCobblemonCompat.isCobblemonEntry(this.entry);
        boolean isTutorial = this.entry instanceof GuideEntry ge && ge.isVirtual() && ge.virtualData() != null && "tutorial".equals(ge.virtualData().virtualType());

        if (!entityInitialized) {
            if (this.entry instanceof GuideEntry ge && ge.hasVisualVariants()) {
                visualVariant = resolveVisualVariant(ge);
            }

            if (visualVariant != null && visualVariant.displayType() == EntryVariantData.DisplayType.ENTITY) {
                if (Minecraft.getInstance().level != null) {
                    cachedEntity = EntryRenderHelper.createVariantEntity(Minecraft.getInstance().level, visualVariant.displayId(), visualVariant.nbt());
                }
            } else if (isCobblemon) {
                ResourceLocation id = ((GuideEntry) this.entry).id();
                cobblemonForm = (variantId != null && !variantId.isEmpty()) ? variantId : ClientFieldGuideCobblemonCompat.getFormForEntry(id);
                cachedEntity = ClientFieldGuideCobblemonCompat.getDummyVariant(id, cobblemonForm, Minecraft.getInstance().level);
            } else if (coreEntry instanceof EntityType<?> type) {
                cachedEntity = DummyEntities.create(type, Minecraft.getInstance().level);

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
            EntryRenderHelper.renderCobblemonForm(guiGraphics, (GuideEntry) this.entry, cobblemonForm, iconX, iconY, 24, 24, true, false, 1.0F);
        } else if (isTutorial) {
            EntryRenderHelper.renderTutorial(guiGraphics, (GuideEntry) this.entry, iconX, iconY, 24, 24, true, false, 1.0F);
        } else if (coreEntry instanceof EntityType<?>) {
            if (cachedEntity != null) {
                EntryRenderHelper.renderEntityNormalized(guiGraphics, cachedEntity, iconX, iconY, 24, 24, true, false, 1.0F, false);
            } else {
                GuiCompat.blit(guiGraphics, Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
            }
        } else if (coreEntry instanceof Block block) {
            EntryRenderHelper.renderBlock(guiGraphics, block, iconX, iconY, 12.0F, true, false, 1.0F);
        } else if (coreEntry instanceof Item item) {
            EntryRenderHelper.renderItem(guiGraphics, item, iconX, iconY, 20.0F, true, false, 1.0F);
        } else {
            GuiCompat.blit(guiGraphics, Constants.TOAST_ICON, 8, 8, 0, 0, 16, 16, 16, 16);
        }

        //? if <26.1 {
        return timeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
        //?}
    }

    private EntryVariantData resolveVisualVariant(GuideEntry ge) {
        String target = (this.variantId != null && !this.variantId.isEmpty())
                ? this.variantId
                : ProgressManager.getInstance().getSelectedVariant(ge.id());
        if (target != null) {
            for (EntryVariantData vd : ge.visualVariants()) {
                if (vd.variantId().equals(target)) return vd;
            }
        }
        return ge.visualVariants().isEmpty() ? null : ge.visualVariants().get(0);
    }
}

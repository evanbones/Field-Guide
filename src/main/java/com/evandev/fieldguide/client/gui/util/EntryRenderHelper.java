package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.data.EntryVisual;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.etf.EtfCompat;
import com.evandev.fieldguide.compat.tide.ClientTideCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.mixin.accessor.EntityAccessor;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.structure.StructureUtils;
import com.evandev.fieldguide.util.DummyEntities;
import com.evandev.fieldguide.util.EntityNbt;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.awt.*;
import java.util.*;
import java.util.List;

//? if <26.1 {
import com.evandev.fieldguide.compat.emf.EmfCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.joml.Matrix4f;
import org.joml.Vector3f;
//?}

//? if >=26.1 {
/*import com.evandev.fieldguide.client.render.FullbrightNodeCollector;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.animal.fish.WaterAnimal;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
*///?}

public class EntryRenderHelper {

    private static final Map<String, Optional<ResourceLocation>> OVERRIDE_CACHE = new HashMap<>();
    //? if >=26.1 {
    /*private static final Set<ResourceLocation> GENERATED_RP_SILHOUETTES = ConcurrentHashMap.newKeySet();
    *///?}
    //? if >=26.2 {
    /*private static final int DISPLAY_ENTITY_ID = 1;
    *///?}

    public static void clearCache() {
        OVERRIDE_CACHE.clear();
        //? if >=26.1 {
        /*GENERATED_RP_SILHOUETTES.clear();
        *///?}
        IconCacheManager.clearCache();
        if (Services.PLATFORM.isModLoaded("cobblemon")) {
            ClientFieldGuideCobblemonCompat.clearCache();
        }
    }

    private static Optional<ResourceLocation> getResourcePackOverride(Object baseEntry, Object cacheKey, boolean isPage) {
        String entryKey = AutoPopulateRegistry.getEntryKey(baseEntry);
        String key = entryKey + (cacheKey instanceof String str && str.contains("#") ? str.substring(str.indexOf("#")) : "") + (isPage ? "_page" : "_grid");
        if (OVERRIDE_CACHE.containsKey(key)) {
            return OVERRIDE_CACHE.get(key);
        }
        ResourceLocation id = AutoPopulateRegistry.getEntryId(baseEntry);
        if (id != null && !entryKey.isEmpty()) {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            String primaryPrefix = entryKey.replace(":", "_").replace("/", "_");

            List<String> prefixCandidates = new ArrayList<>();
            prefixCandidates.add(primaryPrefix);

            if (primaryPrefix.startsWith("item_")) {
                String entityPrefix = "entity_" + primaryPrefix.substring("item_".length());
                if (!prefixCandidates.contains(entityPrefix)) {
                    prefixCandidates.add(entityPrefix);
                }
            } else if (primaryPrefix.startsWith("entity_")) {
                String itemPrefix = "item_" + primaryPrefix.substring("entity_".length());
                if (!prefixCandidates.contains(itemPrefix)) {
                    prefixCandidates.add(itemPrefix);
                }
            }

            String nsPath = id.getNamespace() + "_" + id.getPath();
            if (!prefixCandidates.contains(nsPath)) {
                prefixCandidates.add(nsPath);
            }
            if (!prefixCandidates.contains(id.getPath())) {
                prefixCandidates.add(id.getPath());
            }

            String variantId = null;
            if (cacheKey instanceof String str && str.contains("#")) {
                variantId = str.substring(str.indexOf('#') + 1).replace(":", "_").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._\\-]", "_");
            }

            for (String prefix : prefixCandidates) {
                if (variantId != null) {
                    ResourceLocation specificVariantLoc = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + prefix + "_" + variantId + (isPage ? "_page.png" : "_grid.png"));
                    if (resourceManager.getResource(specificVariantLoc).isPresent()) {
                        OVERRIDE_CACHE.put(key, Optional.of(specificVariantLoc));
                        return Optional.of(specificVariantLoc);
                    }

                    ResourceLocation genericVariantLoc = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + prefix + "_" + variantId + ".png");
                    if (resourceManager.getResource(genericVariantLoc).isPresent()) {
                        OVERRIDE_CACHE.put(key, Optional.of(genericVariantLoc));
                        return Optional.of(genericVariantLoc);
                    }
                }

                ResourceLocation specificLoc = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + prefix + (isPage ? "_page.png" : "_grid.png"));
                ResourceLocation defaultLoc = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + prefix + ".png");

                if (resourceManager.getResource(specificLoc).isPresent()) {
                    OVERRIDE_CACHE.put(key, Optional.of(specificLoc));
                    return Optional.of(specificLoc);
                } else if (resourceManager.getResource(defaultLoc).isPresent()) {
                    OVERRIDE_CACHE.put(key, Optional.of(defaultLoc));
                    return Optional.of(defaultLoc);
                }
            }
        }
        OVERRIDE_CACHE.put(key, Optional.empty());
        return Optional.empty();
    }

    private static void renderWithCache(Object baseEntry, Object cacheKey, GuiGraphics guiGraphics, int x, int y, int width, int height, boolean unlocked, boolean isPage, float bounceScale, IconCacheManager.RenderAction renderAction) {
        Optional<ResourceLocation> textureOpt = getResourcePackOverride(baseEntry, cacheKey, isPage);

        if (textureOpt.isEmpty()) {
            textureOpt = IconCacheManager.getOrGenerateIcon(baseEntry, cacheKey, isPage, renderAction);
        }

        textureOpt.ifPresent(texture -> drawCachedTexture(guiGraphics, texture, x, y, width, height, unlocked, isPage, bounceScale));
    }

    public static void renderEntityNormalized(GuiGraphics guiGraphics, Entity entity, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale) {
        renderEntityNormalized(guiGraphics, entity, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, !isPage);
    }

    public static void renderEntityNormalized(GuiGraphics guiGraphics, Entity entity, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale, String explicitVariantId) {
        ResourceLocation baseId = AutoPopulateRegistry.getEntryId(entity.getType());
        boolean isCobblemon = FieldGuideCobblemonCompat.isPokemon(entity);
        if (isCobblemon) {
            baseId = FieldGuideCobblemonCompat.getPokemonEntryId(entity);
        }
        Object baseEntry = isCobblemon ? baseId : entity.getType();
        Object cacheKey = (explicitVariantId == null || explicitVariantId.isEmpty()) ? baseId : baseId.toString() + "#" + explicitVariantId;
        renderWithCache(baseEntry, cacheKey, guiGraphics, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, (pose, output) -> renderEntity(entity, entity.getType(), isPage, -30.0F, pose, output, null, null));
    }

    public static void renderEntityNormalized(GuiGraphics guiGraphics, Entity entity, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale, boolean syncWithProgress) {
        String variantId = "";
        VariantProvider<Mob> provider = null;
        VariantDef currentVariant = null;

        if (entity instanceof Mob mob) {
            provider = FieldGuideVariantManager.getProvider(mob);
            if (provider != null) {
                if (syncWithProgress) {
                    ResourceLocation entryId = ClientFieldGuideManager.getEntryId(mob.getType());
                    String selected = ProgressManager.getInstance().getSelectedVariant(entryId);
                    if (selected != null) {
                        for (VariantDef def : provider.getVariants(mob)) {
                            if (def.id().equals(selected)) {
                                currentVariant = def;
                                variantId = selected;
                                break;
                            }
                        }
                    }
                }

                if (currentVariant == null) {
                    currentVariant = provider.getCurrent(mob);
                    variantId = currentVariant.id();
                }
            }
        }

        ResourceLocation baseId = AutoPopulateRegistry.getEntryId(entity.getType());
        boolean isCobblemon = FieldGuideCobblemonCompat.isPokemon(entity);
        if (isCobblemon) {
            baseId = FieldGuideCobblemonCompat.getPokemonEntryId(entity);
        }

        String finalVariantId = variantId;
        if (isCobblemon) {
            if (finalVariantId.isEmpty()) {
                finalVariantId = ClientFieldGuideCobblemonCompat.getFormForEntry(baseId);
            }
        }
        Object baseEntry = isCobblemon ? baseId : entity.getType();
        Object cacheKey = finalVariantId.isEmpty() ? baseId : baseId.toString() + "#" + finalVariantId;

        final VariantProvider<Mob> finalProvider = provider;
        final VariantDef finalVariant = currentVariant;

        renderWithCache(baseEntry, cacheKey, guiGraphics, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, (pose, output) -> {
            VariantDef tempOriginal = null;

            if (finalProvider != null && entity instanceof Mob mob) {
                tempOriginal = finalProvider.getCurrent(mob);
                finalProvider.apply(mob, finalVariant);
            }

            renderEntity(entity, entity.getType(), isPage, -30.0F, pose, output, finalVariant, finalProvider);

            if (finalProvider != null && entity instanceof Mob mob && tempOriginal != null) {
                finalProvider.apply(mob, tempOriginal);
            }
        });
    }

    public static void renderCobblemon(GuiGraphics guiGraphics, GuideEntry entry, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale) {
        renderCobblemon(guiGraphics, entry, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, !isPage);
    }

    public static void renderCobblemon(GuiGraphics guiGraphics, GuideEntry entry, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale, boolean syncWithProgress) {
        String formName;
        if (syncWithProgress) {
            formName = ClientFieldGuideCobblemonCompat.getFormForEntry(entry.id());
        } else {
            LivingEntity dummy = ClientFieldGuideCobblemonCompat.getDummyPokemon(entry.id(), Minecraft.getInstance().level);
            formName = FieldGuideCobblemonCompat.getCurrentForm(dummy);
        }
        Object cacheKey = formName.equalsIgnoreCase("standard") ? entry : entry.id().toString() + "#" + formName;

        renderWithCache(entry, cacheKey, guiGraphics, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, (pose, output) -> {
            ResourceLocation id = entry.id();
            LivingEntity dummy = ClientFieldGuideCobblemonCompat.getDummyPokemon(id, Minecraft.getInstance().level);
            if (dummy != null) {
                renderEntity(dummy, id, isPage, -30.0F, pose, output, null, null);
            }
        });
    }

    public static void renderTutorial(GuiGraphics guiGraphics, GuideEntry entry, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale) {
        ResourceLocation texture = entry.icon();
        if (texture == null) texture = Constants.DEFAULT_ICON;

        drawCachedTexture(guiGraphics, texture, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale);
    }

    //? if <26.1 {
    private static void renderEntity(Entity entity, Object entrySource, boolean isPage, float yRotation, PoseStack pose, MultiBufferSource.BufferSource output, VariantDef variantDef, VariantProvider<Mob> provider) {
        setupFieldGuideEntityLighting();
    //?} else {
    /*@SuppressWarnings("unchecked")
    private static <T extends Entity, S extends EntityRenderState> void renderEntity(T entity, Object entrySource, boolean isPage, float yRotation, PoseStack pose, SubmitNodeCollector output, VariantDef variantDef, VariantProvider<Mob> provider) {
    *///?}
        EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(entrySource);

        float visualScale = getVisualScale(visual, isPage);
        float yOff = getYOffset(visual, isPage);
        float xOff = getXOffset(visual, isPage);

        float dynamicFactor = getScaleFactorForEntity(entity);
        float clampedScale = 85.0F * dynamicFactor * visualScale;
        float entityHeight = entity.getBbHeight();
        float entityWidth = entity.getBbWidth();
        float maxDimension = Math.max(entityHeight, entityWidth);

        float safetyClamp = isPage ? 250.0F : 230.0F;
        if (maxDimension * clampedScale > safetyClamp) {
            clampedScale = safetyClamp / maxDimension;
        }

        pose.pushPose();
        //? if <26.1 {
        pose.scale(clampedScale, clampedScale, clampedScale);
        //?} else {
        /*pose.scale(clampedScale, -clampedScale, clampedScale);
        *///?}
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(yRotation));
        pose.translate((xOff / clampedScale), (entityHeight / -2.0F) + (yOff / clampedScale), 0);

        entity.setYRot(0.0F);
        entity.yRotO = 0.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(0.0F);
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
            living.setYBodyRot(0.0F);
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.walkAnimation.setSpeed(0.0F);
            living.walkAnimation.position(0.0F);
            //? if <26.3 {
            living.attackAnim = 0.0F;
            living.oAttackAnim = 0.0F;
            //?}
        }

        if (Minecraft.getInstance().player != null) {
            entity.tickCount = Minecraft.getInstance().player.tickCount;
        } else {
            entity.tickCount = 1;
        }

        if (entity instanceof WaterAnimal) {
            ((EntityAccessor) entity).fieldguide$setWasTouchingWater(true);
        }

        //? if >=26.2 {
        /*if (((EntityAccessor) entity).fieldguide$rawId() == 0) {
            entity.setId(DISPLAY_ENTITY_ID);
        }
        *///?}

        if (Services.PLATFORM.isModLoaded("tide")) {
            ClientTideCompat.applyLavaFishFix(entity);
        }

        if (entity.blockPosition().equals(BlockPos.ZERO) && Services.PLATFORM.isModLoaded("entity_texture_features")) {
            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            EtfCompat.markAsDisplayEntity(entity, typeId.hashCode());
        }

        //? if <26.1 {
        if (Services.PLATFORM.isModLoaded("entity_model_features")) {
            try {
                EmfCompat.setInGui(true);
            } catch (Throwable ignored) {
        //?} else {
        /*try {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderer<T, S> renderer = (EntityRenderer<T, S>) dispatcher.getRenderer(entity);

            S state = renderer.createRenderState(entity, 0.0F);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;

            if (provider != null && variantDef != null && entity instanceof Mob mob) {
                provider.applyToRenderState(mob, state, variantDef);
        *///?}
            }
        //? if <26.1 {
        }
        //?}

        //? if <26.1 {
        var entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        boolean previousHitboxState = entityRenderDispatcher.shouldRenderHitBoxes();
        entityRenderDispatcher.setRenderHitBoxes(false);

        try {
            float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
            entityRenderDispatcher.render(entity, 0, 0, 0, 0.0F, partialTicks, pose, output, LightTexture.FULL_BRIGHT);
        //?} else if <26.2 {
            /*CameraRenderState camera = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
            renderer.submit(state, pose, new FullbrightNodeCollector(output), camera);
        *///?} else {
            /*CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            renderer.submit(state, pose, new FullbrightNodeCollector(output), camera);
        *///?}
        } catch (Exception e) {
            Constants.LOG.error("Failed to render entity in Field Guide: {}", entrySource, e);
        } finally {
            //? if <26.1 {
            entityRenderDispatcher.setRenderHitBoxes(previousHitboxState);
            output.endBatch();

            if (Services.PLATFORM.isModLoaded("entity_model_features")) {
                try {
                    EmfCompat.setInGui(false);
                } catch (Throwable ignored) {
                }
            }
            //?}
            pose.popPose();
        }
    }

    public static void renderBlock(GuiGraphics guiGraphics, Block block, int x, int y, float baseScale, boolean unlocked, boolean isPage, float bounceScale) {
        //? if >=26.1 {
        /*if (block instanceof EntityBlock) {
            renderItem(guiGraphics, block.asItem(), x, y, baseScale, unlocked, isPage, bounceScale);
            return;
        }
        *///?}

        int scaledSize = (int) (baseScale * 2);

        renderWithCache(block, block, guiGraphics, x, y, scaledSize, scaledSize, unlocked, isPage, bounceScale, (pose, output) -> {
            //? if <26.1 {
            setupFieldGuideBlockLighting();
            //?}
            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(block);

            float clampedScale = 100f * getVisualScale(visual, isPage);

            pose.pushPose();
            //? if <26.1 {
            pose.scale(clampedScale, clampedScale, clampedScale);
            //?} else {
            /*pose.scale(clampedScale, -clampedScale, clampedScale);
            *///?}
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(210.0F));
            pose.translate(-0.5, -0.5, -0.5);

            BlockState state = block.defaultBlockState();

            for (Property<?> prop : state.getProperties()) {
                if (prop instanceof IntegerProperty intProp) {
                    String name = prop.getName();
                    if (!name.equals("bites") && !name.equals("level") && !name.equals("rotation")) {
                        int max = intProp.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
                        state = state.setValue(intProp, max);
                    }
                }
            }

            Property<?> verticalProp = state.getProperties().stream()
                    .filter(p -> p instanceof EnumProperty<?> enumProp && enumProp.getValueClass() == DoubleBlockHalf.class)
                    .findFirst()
                    .orElse(null);

            //? if >=26.1 {
            /*BlockModelResolver resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
            *///?}

            try {
                if (verticalProp == null) {
                    //? if <26.1 {
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, output, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    //?} else {
                    /*submitBlock(resolver, state, pose, output);
                    *///?}
                } else {
                    Collection<?> values = verticalProp.getPossibleValues();
                    pose.translate(0.0F, -0.5F * (values.size() - 1), 0.0F);

                    List<?> sortedValues = values.stream()
                            .sorted((a, b) -> Integer.compare(((Enum<?>) b).ordinal(), ((Enum<?>) a).ordinal()))
                            .toList();

                    for (Object value : sortedValues) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        BlockState variant = state.setValue((Property) verticalProp, (Comparable) value);
                        //? if <26.1 {
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(variant, pose, output, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        //?} else {
                        /*submitBlock(resolver, variant, pose, output);
                        *///?}
                        pose.translate(0.0F, 1.0F, 0.0F);
                //? if <26.1 {
                    }
                }

                if (block instanceof EntityBlock entityBlock) {
                    var blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                    if (blockEntity != null) {
                        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(blockEntity, pose, output, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                //?}
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to render block in Field Guide: {}", BuiltInRegistries.BLOCK.getKey(block), e);
            } finally {
                //? if <26.1 {
                output.endBatch();
                //?}
                pose.popPose();
            }
        });
    }

    public static void renderItem(GuiGraphics guiGraphics, Item item, int x, int y, float baseScale, boolean unlocked, boolean isPage, float bounceScale) {
        int scaledSize = (int) (baseScale * 2);

        renderWithCache(item, item, guiGraphics, x, y, scaledSize, scaledSize, unlocked, isPage, bounceScale, (pose, output) -> {
            ItemStack stack = new ItemStack(item);
            //? if <26.1 {
            setupFieldGuideItemLighting();
            //?}

            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(item);

            float clampedScale = 100f * getVisualScale(visual, isPage);

            pose.pushPose();
            //? if <26.1 {
            pose.scale(clampedScale, clampedScale, 1.0f);
            //?} else {
            /*pose.scale(clampedScale, -clampedScale, 1.0f);
            *///?}

            try {
                //? if <26.1 {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, output, Minecraft.getInstance().level, 0);
                //?} else {
                /*ItemStackRenderState state = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);
                state.submit(pose, output, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                *///?}
            } catch (Exception e) {
                Constants.LOG.error("Failed to render item in Field Guide: {}", BuiltInRegistries.ITEM.getKey(item), e);
            } finally {
                //? if <26.1 {
                output.endBatch();
                //?}
                pose.popPose();
            }
        });
    }

    public static Entity createVariantEntity(Level level, ResourceLocation entityId, CompoundTag nbt) {
        if (level == null || entityId == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) return null;
        Entity entity = DummyEntities.create(type, level);
        if (entity != null && nbt != null && !nbt.isEmpty()) {
            try {
                EntityNbt.load(entity, nbt);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply nbt to variant entity {}: {}", entityId, e.getMessage());
            }
        }
        return entity;
    }

    public static void renderVisualVariant(GuiGraphics guiGraphics, GuideEntry baseEntry, EntryVariantData vd, Entity cachedEntity, int centerX, int centerY, int size, boolean unlocked, boolean isPage, float bounceScale) {
        switch (vd.displayType()) {
            case ENTITY -> {
                if (cachedEntity != null) {
                    int box = isPage ? 112 : size - 8;
                    renderEntityNormalized(guiGraphics, cachedEntity, centerX, centerY, box, box, unlocked, isPage, bounceScale, vd.variantId());
                }
            }
            case BLOCK -> BuiltInRegistries.BLOCK.getOptional(vd.displayId()).ifPresent(block ->
                    renderBlock(guiGraphics, block, centerX, centerY, isPage ? 40.0F : size * 0.42F, unlocked, isPage, bounceScale));
            case ITEM -> BuiltInRegistries.ITEM.getOptional(vd.displayId()).ifPresent(item ->
                    renderItem(guiGraphics, item, centerX, centerY, isPage ? 60.0F : size * 0.5F, unlocked, isPage, bounceScale));
            case STRUCTURE -> {
                if (vd.structureData() != null) {
                    ResourceLocation dummyId = ResourceLocation.fromNamespaceAndPath(baseEntry.id().getNamespace(), baseEntry.id().getPath() + "_" + vd.variantId().replace(":", "_"));
                    GuideEntry dummy = new GuideEntry(dummyId, baseEntry.displayId(), vd.icon(), baseEntry.kind(), baseEntry.virtual(), baseEntry.autoPopulate(), baseEntry.strategy(), baseEntry.childEntries(), vd.structureData(), null, baseEntry.virtualData(), baseEntry.unlockData(), null, null);
                    renderStructure(guiGraphics, dummy, centerX, centerY, isPage ? size : size - 4, unlocked, isPage, bounceScale);
                }
            }
        }
    }

    public static void renderStructure(GuiGraphics guiGraphics, GuideEntry composite, int x, int y, int size, boolean unlocked, boolean isPage, float bounceScale) {
        renderWithCache(composite, composite, guiGraphics, x, y, size, size, unlocked, isPage, bounceScale, (pose, output) -> {
            //? if <26.1 {
            setupFieldGuideBlockLighting();
            //?}

            Map<BlockPos, BlockState> blocks = StructureUtils.getStructureBlocks(composite);
            if (blocks.isEmpty() && composite.structureData() != null && composite.structureData().stackedBlocks() != null && !composite.structureData().stackedBlocks().isEmpty()) {
                blocks = StructureUtils.getStackedBlocks(composite.structureData().stackedBlocks());
            }
            if (blocks.isEmpty()) return;

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (BlockPos pos : blocks.keySet()) {
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int length = maxZ - minZ + 1;
            int maxDim = Math.max(width, Math.max(height, length));

            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(composite);
            float visualScale = getVisualScale(visual, isPage);
            float xOff = getXOffset(visual, isPage);
            float yOff = getYOffset(visual, isPage);

            float scale = 35.0f * (5.0f / maxDim) * visualScale;
            pose.pushPose();
            //? if <26.1 {
            pose.scale(scale, scale, scale);
            //?} else {
            /*pose.scale(scale, -scale, scale);
            *///?}

            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(210.0F));

            float centerX = minX + width / 2.0f;
            float centerY = minY + height / 2.0f;
            float centerZ = minZ + length / 2.0f;

            pose.translate(-centerX + (xOff / scale), -centerY + (yOff / scale), -centerZ);

            //? if <26.1 {
            var blockRenderer = Minecraft.getInstance().getBlockRenderer();
            //?} else {
            /*BlockModelResolver resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
            *///?}

            try {
                for (Map.Entry<BlockPos, BlockState> b : blocks.entrySet()) {
                    BlockPos pos = b.getKey();
                    BlockState state = b.getValue();
                    if (state.isAir()) continue;

                    pose.pushPose();
                    pose.translate(pos.getX(), pos.getY(), pos.getZ());
                    //? if <26.1 {
                    blockRenderer.renderSingleBlock(state, pose, output, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    //?} else {
                    /*submitBlock(resolver, state, pose, output);
                    *///?}
                    pose.popPose();
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to render structure in Field Guide", e);
            } finally {
                //? if <26.1 {
                output.endBatch();
                //?}
                pose.popPose();
            }
        });
    }

    private static float getScaleFactorForEntity(Entity entity) {
        try {
            float width = entity.getBbWidth();
            float height = entity.getBbHeight();
            float referenceSize = Math.max(width, height);

            if (referenceSize <= 0.01F) {
                return 1.0F;
            }

            float scaleFactor = (float) Math.sqrt(1.8F / referenceSize);
            float minScale = 0.2F;
            float maxScale = 3.5F;

            return Math.max(minScale, Math.min(scaleFactor, maxScale));

        } catch (Exception e) {
            return 1.0F;
        }
    }

    private static float getVisualScale(EntryVisual visual, boolean isPage) {
        if (isPage && visual.pageScale != null) return visual.pageScale;
        if (!isPage && visual.gridScale != null) return visual.gridScale;
        return visual.scale;
    }

    private static float getYOffset(EntryVisual visual, boolean isPage) {
        if (isPage && visual.pageYOffset != null) return visual.pageYOffset;
        if (!isPage && visual.gridYOffset != null) return visual.gridYOffset;
        return visual.yOffset;
    }

    private static float getXOffset(EntryVisual visual, boolean isPage) {
        if (isPage && visual.pageXOffset != null) return visual.pageXOffset;
        if (!isPage && visual.gridXOffset != null) return visual.gridXOffset;
        return visual.xOffset;
    }

    //? if <26.1 {
    private static void setupFieldGuideEntityLighting() {
        Vector3f light0 = new Vector3f(-1.0F, 1.0F, 1.0F).normalize();
        Vector3f light1 = new Vector3f(1.0F, 1.0F, 1.0F).normalize();
        RenderSystem.setShaderLights(light0, light1);
    //?} else {
    /*private static void submitBlock(BlockModelResolver resolver, BlockState state, PoseStack pose, SubmitNodeCollector collector) {
        BlockModelRenderState renderState = new BlockModelRenderState();
        resolver.update(renderState, state, BlockDisplayContext.create());
        renderState.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    *///?}
    }

    //? if <26.1 {
    private static void setupFieldGuideBlockLighting() {
        Vector3f light0 = new Vector3f(-0.2F, 1.0F, -0.7F).normalize();
        Vector3f light1 = new Vector3f(0.2F, 0.0F, 0.7F).normalize();
        RenderSystem.setShaderLights(light0, light1);
    }
    //?} else {
    /*private static ResourceLocation getOrCreateResourcePackSilhouette(ResourceLocation baseTexture) {
        String path = baseTexture.getPath();
        String silPath = path.endsWith(".png") ? path.replace(".png", "_silhouette.png") : path + "_silhouette";
        ResourceLocation silLoc = ResourceLocation.fromNamespaceAndPath(baseTexture.getNamespace(), silPath);
    *///?}

    //? if <26.1 {
    private static void setupFieldGuideItemLighting() {
        Matrix4f guiFlat = new Matrix4f().rotationY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
        Vector3f light0 = guiFlat.transformDirection(new Vector3f(0.2F, 1.0F, -0.7F).normalize(), new Vector3f()).mul(1.0F, -1.0F, 1.0F);
        Vector3f light1 = guiFlat.transformDirection(new Vector3f(-0.2F, 1.0F, 0.7F).normalize(), new Vector3f()).mul(1.0F, -1.0F, 1.0F);
        RenderSystem.setShaderLights(light0, light1);
    //?} else {
        /*if (GENERATED_RP_SILHOUETTES.contains(silLoc)) {
            return silLoc;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager().getResource(silLoc).isPresent()) {
            return silLoc;
        }

        try {
            var resource = mc.getResourceManager().getResource(baseTexture);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                    NativeImage silhouetteImage = new NativeImage(image.getWidth(), image.getHeight(), false);
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int alpha = ARGB.alpha(image.getPixel(x, y));
                            silhouetteImage.setPixel(x, y, alpha > 0 ? ARGB.color(alpha, 255, 255, 255) : 0);
                        }
                    }
                    mc.getTextureManager().register(silLoc, new DynamicTexture(silLoc::toString, silhouetteImage));
                    GENERATED_RP_SILHOUETTES.add(silLoc);
                    return silLoc;
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate silhouette for resource pack texture: {}", baseTexture, e);
        }

        return baseTexture;
    *///?}
    }

    private static void drawCachedTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int width, int height, boolean unlocked, boolean isPage, float bounceScale) {
        int scaledWidth = (int) (width * bounceScale);
        int scaledHeight = (int) (height * bounceScale);
        int drawX = x - scaledWidth / 2;
        int drawY = y - scaledHeight / 2;

        boolean silhouette = !unlocked || ServerConfig.get().keepSilhouetteWhenUnlocked;

        if (silhouette) {
            //? if <26.1 {
            guiGraphics.flush();
            //?}

            int color;
            double alpha;
            if (isPage) {
                color = unlocked ? ClientConfig.get().getDetailsSilhouetteColorInt() : ClientConfig.get().getDetailsUnlockedSilhouetteColorInt();
                alpha = unlocked ? ClientConfig.get().detailsUnlockedSilhouetteAlpha : ClientConfig.get().detailsSilhouetteAlpha;
            } else {
                color = unlocked ? ClientConfig.get().getListSilhouetteColorInt() : ClientConfig.get().getListUnlockedSilhouetteColorInt();
                alpha = unlocked ? ClientConfig.get().listUnlockedSilhouetteAlpha : ClientConfig.get().listSilhouetteAlpha;
            }

            Color rgb = new Color(color);
            //? if <26.1 {
            float r = rgb.getRed() / 255F;
            float g = rgb.getGreen() / 255F;
            float b = rgb.getBlue() / 255F;
            //?} else {
            /*int argb = ARGB.color((int) (alpha * 255), rgb.getRed(), rgb.getGreen(), rgb.getBlue());
            *///?}

            //? if <26.1 {
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderFogColor(r, g, b, (float) alpha);
            RenderSystem.setShaderFogStart(0.0F);
            RenderSystem.setShaderFogEnd(0.1F);

            guiGraphics.flush();

            VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.entityCutout(texture));
            Matrix4f matrix = guiGraphics.pose().last().pose();

            consumer.addVertex(matrix, drawX, drawY + scaledHeight, 0).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, drawX + scaledWidth, drawY + scaledHeight, 0).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, drawX + scaledWidth, drawY, 0).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
            consumer.addVertex(matrix, drawX, drawY, 0).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);

            guiGraphics.flush();

            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            RenderSystem.disableDepthTest();
            //?} else {
            /*ResourceLocation silhouetteTexture = texture.getPath().startsWith("generated_icon/")
                    ? ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "_silhouette")
                    : getOrCreateResourcePackSilhouette(texture);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, silhouetteTexture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight, argb);
            *///?}
        } else {
            //? if <26.1 {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            guiGraphics.blit(texture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
            //?} else {
            /*guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight, -1);
            *///?}
        }
    }
}

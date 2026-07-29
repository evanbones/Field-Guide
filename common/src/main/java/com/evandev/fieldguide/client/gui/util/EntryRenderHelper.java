package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.api.variant.VariantProvider;
import com.evandev.fieldguide.client.ClientFieldGuideManager;
import com.evandev.fieldguide.client.data.EntryVisual;
import com.evandev.fieldguide.client.progress.ProgressManager;
import com.evandev.fieldguide.client.render.FullbrightNodeCollector;
import com.evandev.fieldguide.compat.tide.ClientTideCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.mixin.accessor.EntityAccessor;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.structure.StructureUtils;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Quaternionf;

import java.awt.*;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.mojang.blaze3d.platform.Lighting.Entry.ENTITY_IN_UI;

public class EntryRenderHelper {

    private static final Map<String, Optional<Identifier>> OVERRIDE_CACHE = new HashMap<>();
    private static final Set<Identifier> GENERATED_RP_SILHOUETTES = ConcurrentHashMap.newKeySet();

    public static void clearCache() {
        OVERRIDE_CACHE.clear();
        GENERATED_RP_SILHOUETTES.clear();
        IconCacheManager.clearCache();
    }

    private static Optional<Identifier> getResourcePackOverride(Object baseEntry, Object cacheKey, boolean isPage) {
        String entryKey = AutoPopulateRegistry.getEntryKey(baseEntry);
        String key = entryKey + (cacheKey instanceof String str && str.contains("#") ? str.substring(str.indexOf("#")) : "") + (isPage ? "_page" : "_grid");

        if (OVERRIDE_CACHE.containsKey(key)) {
            return OVERRIDE_CACHE.get(key);
        }

        Identifier id = AutoPopulateRegistry.getEntryId(baseEntry);
        if (id != null) {
            if (cacheKey instanceof String str && str.contains("#")) {
                String variantId = str.substring(str.indexOf('#') + 1).replace(":", "_").toLowerCase(Locale.ROOT);
                Identifier specificVariantLoc = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + id.getPath() + "_" + variantId + (isPage ? "_page.png" : "_grid.png"));
                if (Minecraft.getInstance().getResourceManager().getResource(specificVariantLoc).isPresent()) {
                    OVERRIDE_CACHE.put(key, Optional.of(specificVariantLoc));
                    return Optional.of(specificVariantLoc);
                }
            }

            Identifier specificLoc = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + id.getPath() + (isPage ? "_page.png" : "_grid.png"));
            Identifier defaultLoc = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/fieldguide/entries/" + id.getPath() + ".png");

            var resourceManager = Minecraft.getInstance().getResourceManager();
            if (resourceManager.getResource(specificLoc).isPresent()) {
                OVERRIDE_CACHE.put(key, Optional.of(specificLoc));
                return Optional.of(specificLoc);
            } else if (resourceManager.getResource(defaultLoc).isPresent()) {
                OVERRIDE_CACHE.put(key, Optional.of(defaultLoc));
                return Optional.of(defaultLoc);
            }
        }

        OVERRIDE_CACHE.put(key, Optional.empty());
        return Optional.empty();
    }

    private static void renderWithCache(Object baseEntry, Object cacheKey, GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, boolean unlocked, boolean isPage, float bounceScale, BiConsumer<PoseStack, SubmitNodeCollector> renderAction) {
        Optional<Identifier> textureOpt = getResourcePackOverride(baseEntry, cacheKey, isPage);

        if (textureOpt.isEmpty()) {
            textureOpt = IconCacheManager.getOrGenerateIcon(baseEntry, cacheKey, isPage, renderAction);
        }

        textureOpt.ifPresent(texture -> drawCachedTexture(guiGraphics, texture, cacheKey, x, y, width, height, unlocked, isPage, bounceScale));
    }

    public static void renderEntityNormalized(GuiGraphicsExtractor guiGraphics, Entity entity, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale) {
        renderEntityNormalized(guiGraphics, entity, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, !isPage);
    }

    public static void renderEntityNormalized(GuiGraphicsExtractor guiGraphics, Entity entity, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale, boolean syncWithProgress) {
        String variantId = "";
        VariantProvider<Mob> provider = null;
        VariantDef currentVariant = null;

        if (entity instanceof Mob mob) {
            provider = FieldGuideVariantManager.getProvider(mob);
            if (provider != null) {
                if (syncWithProgress) {
                    Identifier entryId = ClientFieldGuideManager.getEntryId(mob.getType());
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

        Identifier baseId = AutoPopulateRegistry.getEntryId(entity.getType());
        String finalVariantId = variantId;
        Object cacheKey = finalVariantId.isEmpty() ? baseId : baseId.toString() + "#" + finalVariantId;

        final VariantProvider<Mob> finalProvider = provider;
        final VariantDef finalVariant = currentVariant;

        renderWithCache(baseId, cacheKey, guiGraphics, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale, (poseStack, collector) -> {

            VariantDef tempOriginal = null;
            if (finalProvider != null && entity instanceof Mob mob) {
                tempOriginal = finalProvider.getCurrent(mob);
                finalProvider.apply(mob, finalVariant);
            }

            renderEntity(entity, entity.getType(), isPage, -30.0F, x, y, maxWidth, maxHeight, bounceScale, poseStack, collector, finalVariant, finalProvider);

            if (finalProvider != null && entity instanceof Mob mob && tempOriginal != null) {
                finalProvider.apply(mob, tempOriginal);
            }
        });
    }

    public static void renderTutorial(GuiGraphicsExtractor guiGraphics, GuideEntry entry, int x, int y, int maxWidth, int maxHeight, boolean unlocked, boolean isPage, float bounceScale) {
        Identifier texture = entry.icon();
        if (texture == null) texture = Constants.DEFAULT_ICON;

        drawCachedTexture(guiGraphics, texture, entry, x, y, maxWidth, maxHeight, unlocked, isPage, bounceScale);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity, S extends EntityRenderState> void renderEntity(T entity, Object entrySource, boolean isPage, float yRotation, int x, int y, int maxWidth, int maxHeight, float bounceScale, PoseStack poseStack, SubmitNodeCollector collector, VariantDef variantDef, VariantProvider<Mob> provider) {
        EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(entrySource);

        float visualScale = getVisualScale(visual, isPage);
        float yOff = getYOffset(visual, isPage);
        float xOff = getXOffset(visual, isPage);

        float dynamicFactor = getScaleFactorForEntity(entity);
        float clampedScale = 85.0F * dynamicFactor * visualScale * bounceScale;
        float entityHeight = entity.getBbHeight();
        float entityWidth = entity.getBbWidth();
        float maxDimension = Math.max(entityHeight, entityWidth);

        float safetyClamp = isPage ? 250.0F : 230.0F;
        if (maxDimension * clampedScale > safetyClamp) {
            clampedScale = safetyClamp / maxDimension;
        }

        poseStack.pushPose();
        poseStack.scale(clampedScale, -clampedScale, clampedScale);
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30.0)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRotation)));
        poseStack.translate(xOff / clampedScale, (entityHeight / -2.0F) + (yOff / clampedScale), 0.0F);

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(0.0F);
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
            living.setYBodyRot(0.0F);
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.walkAnimation.setSpeed(0.0F);
            living.walkAnimation.position(0.0F);
            living.attackAnim = 0.0F;
            living.oAttackAnim = 0.0F;
        }

        entity.tickCount = 1;

        if (entity instanceof WaterAnimal) {
            ((EntityAccessor) entity).fieldguide$setWasTouchingWater(true);
        }

        if (Services.PLATFORM.isModLoaded("tide")) {
            ClientTideCompat.applyLavaFishFix(entity);
        }

        try {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderer<T, S> renderer = (EntityRenderer<T, S>) dispatcher.getRenderer(entity);

            S state = renderer.createRenderState();
            renderer.extractRenderState(entity, state, 0.0F);

            if (provider != null && variantDef != null && entity instanceof Mob mob) {
                provider.applyToRenderState(mob, state, variantDef);
            }

            CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            renderer.submit(state, poseStack, new FullbrightNodeCollector(collector), camera);
        } catch (Exception e) {
            Constants.LOG.error("Failed to render entity in Field Guide: {}", entrySource, e);
        }

        poseStack.popPose();
    }

    public static void renderBlock(GuiGraphicsExtractor guiGraphics, Block block, int x, int y, float baseScale, boolean unlocked, boolean isPage, float bounceScale) {
        if (block instanceof EntityBlock) {
            renderItem(guiGraphics, block.asItem(), x, y, baseScale, unlocked, isPage, bounceScale);
            return;
        }

        int scaledSize = (int) (baseScale * 2);

        renderWithCache(block, block, guiGraphics, x, y, scaledSize, scaledSize, unlocked, isPage, bounceScale, (poseStack, collector) -> {
            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(block);
            float clampedScale = 100f * getVisualScale(visual, isPage) * bounceScale;

            poseStack.pushPose();
            poseStack.scale(clampedScale, -clampedScale, clampedScale);
            poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30.0)));
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(210.0)));
            poseStack.translate(-0.5f, -0.5f, -0.5f);

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

            BlockModelRenderState blockRenderState = new BlockModelRenderState();
            BlockModelResolver resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());

            Property<?> verticalProp = state.getProperties().stream()
                    .filter(p -> p instanceof EnumProperty<?> enumProp && enumProp.getValueClass() == DoubleBlockHalf.class)
                    .findFirst()
                    .orElse(null);

            try {
                if (verticalProp == null) {
                    resolver.update(blockRenderState, state, BlockDisplayContext.create());
                    blockRenderState.submit(poseStack, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                } else {
                    Collection<?> values = verticalProp.getPossibleValues();

                    poseStack.translate(0.0F, -0.5F * (values.size() - 1), 0.0F);

                    List<?> sortedValues = values.stream()
                            .sorted((a, b) -> Integer.compare(((Enum<?>) b).ordinal(), ((Enum<?>) a).ordinal()))
                            .toList();

                    for (Object value : sortedValues) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        BlockState variant = state.setValue((Property) verticalProp, (Comparable) value);

                        BlockModelRenderState variantRenderState = new BlockModelRenderState();
                        resolver.update(variantRenderState, variant, BlockDisplayContext.create());
                        variantRenderState.submit(poseStack, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);

                        poseStack.translate(0.0F, 1.0F, 0.0F);
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to render block in Field Guide: {}", block, e);
            }

            poseStack.popPose();
        });
    }

    public static void renderItem(GuiGraphicsExtractor guiGraphics, Item item, int x, int y, float baseScale, boolean unlocked, boolean isPage, float bounceScale) {
        int scaledSize = (int) (baseScale * 2);

        renderWithCache(item, item, guiGraphics, x, y, scaledSize, scaledSize, unlocked, isPage, bounceScale, (poseStack, collector) -> {
            ItemStack stack = new ItemStack(item);
            EntryVisual visual = ClientFieldGuideManager.getInstance().getEntryVisual(item);

            float clampedScale = 100f * getVisualScale(visual, isPage) * bounceScale;

            poseStack.pushPose();
            poseStack.scale(clampedScale, -clampedScale, 1.0f);

            ItemStackRenderState state = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, Minecraft.getInstance().level, Minecraft.getInstance().player, 0);
            state.submit(poseStack, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        });
    }

    public static void renderStructure(GuiGraphicsExtractor guiGraphics, GuideEntry composite, int x, int y, int size, boolean unlocked, boolean isPage, float bounceScale) {
        renderWithCache(composite, composite, guiGraphics, x, y, size, size, unlocked, isPage, bounceScale, (poseStack, collector) -> {
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

            float scale = 35.0f * (5.0f / maxDim);

            float centerX = minX + width / 2.0f;
            float centerY = minY + height / 2.0f;
            float centerZ = minZ + length / 2.0f;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            poseStack.pushPose();
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(30.0)));
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(210.0)));
            poseStack.translate(-centerX, -centerY, -centerZ);

            List<Map.Entry<BlockPos, BlockState>> sortedBlocks = new ArrayList<>(blocks.entrySet());
            sortedBlocks.sort((a, b) -> {
                BlockPos pA = a.getKey();
                BlockPos pB = b.getKey();
                return Integer.compare(pA.getX() + pA.getY() + pA.getZ(), pB.getX() + pB.getY() + pB.getZ());
            });

            BlockModelRenderState blockRenderState = new BlockModelRenderState();
            BlockModelResolver resolver = new BlockModelResolver(mc.getModelManager());

            for (Map.Entry<BlockPos, BlockState> b : sortedBlocks) {
                BlockPos pos = b.getKey();
                BlockState state = b.getValue();
                if (state.isAir()) continue;

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                resolver.update(blockRenderState, state, BlockDisplayContext.create());
                blockRenderState.submit(poseStack, collector, 15728880, OverlayTexture.NO_OVERLAY, 0);

                poseStack.popPose();
            }

            poseStack.popPose();
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

    private static Identifier getOrCreateResourcePackSilhouette(Identifier baseTexture) {
        String path = baseTexture.getPath();
        String silPath = path.endsWith(".png") ? path.replace(".png", "_silhouette.png") : path + "_silhouette";
        Identifier silLoc = Identifier.fromNamespaceAndPath(baseTexture.getNamespace(), silPath);

        if (GENERATED_RP_SILHOUETTES.contains(silLoc)) {
            return silLoc;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.getResourceManager().getResource(silLoc).isPresent()) {
            return silLoc;
        }

        try {
            var resource = mc.getResourceManager().getResource(baseTexture);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    NativeImage image = NativeImage.read(stream);
                    NativeImage silhouetteImage = new NativeImage(image.getWidth(), image.getHeight(), false);
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int pixel = image.getPixel(x, y);
                            int alpha = ARGB.alpha(pixel);
                            if (alpha > 0) {
                                silhouetteImage.setPixel(x, y, ARGB.color(alpha, 255, 255, 255));
                            } else {
                                silhouetteImage.setPixel(x, y, 0);
                            }
                        }
                    }
                    DynamicTexture dynamicTexture = new DynamicTexture(silLoc::toString, silhouetteImage);
                    mc.getTextureManager().register(silLoc, dynamicTexture);

                    GENERATED_RP_SILHOUETTES.add(silLoc);

                    image.close();
                    return silLoc;
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to generate silhouette for resource pack texture: {}", baseTexture, e);
        }

        return baseTexture;
    }

    private static void drawCachedTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, Object cacheKey, int x, int y, int width, int height, boolean unlocked, boolean isPage, float bounceScale) {
        int scaledWidth = (int) (width * bounceScale);
        int scaledHeight = (int) (height * bounceScale);
        int drawX = x - scaledWidth / 2;
        int drawY = y - scaledHeight / 2;

        boolean silhouette = !unlocked || ServerConfig.get().keepSilhouetteWhenUnlocked;
        Identifier targetTexture = texture;

        if (silhouette) {
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
            int argb = ARGB.color((int) (alpha * 255), rgb.getRed(), rgb.getGreen(), rgb.getBlue());

            if (texture.getPath().startsWith("generated_icon/")) {
                targetTexture = Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + "_silhouette");
            } else {
                targetTexture = getOrCreateResourcePackSilhouette(texture);
            }

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, targetTexture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight, argb);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, targetTexture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight, -1);
        }
    }
}
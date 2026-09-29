package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.compat.emf.EmfCompat;
import com.evandev.fieldguide.platform.Services;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

//? if <26.1 {
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
//?}

//? if >=1.21 && <26.1 {
import org.joml.Matrix4fStack;
//?}

//? if >=26.1 && <26.3 {
/*import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
*///?}

//? if >=26.1 {
/*import com.evandev.fieldguide.entry.EntryResolver;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.IntUnaryOperator;
*///?}

//? if >=26.2 && <26.3 {
/*import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
*///?}

//? if >=26.2 {
/*import org.joml.Vector4f;
*///?}

//? if >=26.3 {
/*import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
*///?}

public class IconCacheManager {
    private static final Path CACHE_DIR = Services.PLATFORM.getConfigDirectory().resolve("../fieldguide_cache");
    private static final int RENDER_SIZE = 256;
    private static final int CACHE_FORMAT = 5;

    @FunctionalInterface
    public interface RenderAction {
        //? if <26.1 {
        void render(PoseStack pose, MultiBufferSource.BufferSource output);
        //?} else {
        /*void render(PoseStack pose, SubmitNodeCollector output);
        *///?}
    }
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_GENERATIONS = ConcurrentHashMap.newKeySet();
    private static final Deque<Runnable> MAIN_THREAD_TASKS = new ConcurrentLinkedDeque<>();
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()), r -> {
        Thread t = new Thread(r, "FieldGuide-IconCache-IO");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean formatChecked = false;

    public static void tick() {
        long startTime = System.currentTimeMillis();
        int processed = 0;

        while (!MAIN_THREAD_TASKS.isEmpty() && processed < 5 && (System.currentTimeMillis() - startTime) < 10) {
            Runnable task = MAIN_THREAD_TASKS.pollFirst();
            if (task != null) {
                task.run();
                processed++;
            }
        }
    }

    public static void init() {
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create icon cache directory", e);
        }
    }

    public static void clearCache() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation id : TEXTURE_CACHE.values()) {
            mc.getTextureManager().release(id);
        }
        TEXTURE_CACHE.clear();
        PENDING_GENERATIONS.clear();
        MAIN_THREAD_TASKS.clear();

        CompletableFuture.runAsync(() -> {
            if (Files.exists(CACHE_DIR)) {
                try (Stream<Path> walk = Files.walk(CACHE_DIR)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(f -> {
                        if (!f.delete()) {
                            Constants.LOG.warn("Could not delete file: {}", f);
                        }
                    });
                } catch (IOException e) {
                    Constants.LOG.error("Failed to delete icon cache directory", e);
                }
            }
        }, IO_EXECUTOR);
    }

    private static synchronized void ensureCacheFormat() {
        if (formatChecked) return;
        formatChecked = true;
        try {
            Files.createDirectories(CACHE_DIR);
            Path versionFile = CACHE_DIR.resolve("cache_format");
            String existing = Files.exists(versionFile) ? Files.readString(versionFile).trim() : "";
            if (!existing.equals(String.valueOf(CACHE_FORMAT))) {
                try (Stream<Path> walk = Files.walk(CACHE_DIR)) {
                    walk.sorted(Comparator.reverseOrder())
                            .filter(p -> !p.equals(CACHE_DIR))
                            .map(Path::toFile)
                            .forEach(f -> {
                                if (!f.delete()) {
                                    Constants.LOG.warn("Could not delete file: {}", f);
                                }
                            });
                }
                Files.createDirectories(CACHE_DIR);
                Files.writeString(versionFile, String.valueOf(CACHE_FORMAT));
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to verify icon cache format", e);
        }
    }

    public static Optional<ResourceLocation> getOrGenerateIcon(Object baseEntry, Object cacheKey, boolean isPage, RenderAction renderAction) {
        ensureCacheFormat();
        String entryKey = AutoPopulateRegistry.getEntryKey(baseEntry);
        if (entryKey.isEmpty()) return Optional.empty();

        String variantSuffix;
        String cacheKeyStr = cacheKey.toString();
        if (cacheKeyStr.contains("#")) {
            variantSuffix = "_" + cacheKeyStr.substring(cacheKeyStr.indexOf('#') + 1).replace(":", "_").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._\\-]", "_");
        } else {
            variantSuffix = "";
        }

        String fileName = (entryKey.replace(":", "_").replace("/", "_") + variantSuffix + (isPage ? "_page" : "_grid") + ".png").toLowerCase(Locale.ROOT);
        String key = (entryKey.replace(":", "_").replace("/", "_") + variantSuffix + (isPage ? "_page" : "_grid")).toLowerCase(Locale.ROOT);

        if (TEXTURE_CACHE.containsKey(key)) {
            return Optional.of(TEXTURE_CACHE.get(key));
        }

        if (!PENDING_GENERATIONS.add(key)) {
            return Optional.empty();
        }

        CompletableFuture.supplyAsync(() -> {
            if (!Files.exists(CACHE_DIR)) init();
            ResourceLocation id = AutoPopulateRegistry.getEntryId(baseEntry);
            if (id == null) return null;

            Path cachedFilePath = CACHE_DIR.resolve(id.getNamespace()).resolve("textures/fieldguide/entries").resolve(fileName);
            File cachedFile = cachedFilePath.toFile();
            if (cachedFile.exists()) {
                try {
                    return NativeImage.read(Files.newInputStream(cachedFile.toPath()));
                } catch (IOException e) {
                    Constants.LOG.error("Failed to load cached icon: {}", key, e);
                }
            }

            String genericFileName = (entryKey.replace(":", "_").replace("/", "_") + variantSuffix + ".png").toLowerCase(Locale.ROOT);
            Path genericFilePath = CACHE_DIR.resolve(id.getNamespace()).resolve("textures/fieldguide/entries").resolve(genericFileName);
            File genericFile = genericFilePath.toFile();
            if (genericFile.exists()) {
                try {
                    return NativeImage.read(Files.newInputStream(genericFile.toPath()));
                } catch (IOException e) {
                    Constants.LOG.error("Failed to load generic cached icon: {}", key, e);
                }
            }

            return null;
        }, IO_EXECUTOR).thenAcceptAsync(image -> {
            if (image != null) {
                //? if <26.1 {
                DynamicTexture texture = new DynamicTexture(image);
                //?}
                ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key);
                //? if <26.1 {
                Minecraft.getInstance().getTextureManager().register(texLoc, texture);
                TEXTURE_CACHE.put(key, texLoc);
                //?} else {
                /*mcRegisterWithSilhouette(key, texLoc, image);
                *///?}
                PENDING_GENERATIONS.remove(key);
            } else {
                ResourceLocation id = AutoPopulateRegistry.getEntryId(baseEntry);
                if (id != null) {
                    //? if <26.1 {
                    MAIN_THREAD_TASKS.addFirst(() -> generateAndSaveIcon(id.getNamespace(), fileName, key, renderAction));
                    //?} else {
                    /*MAIN_THREAD_TASKS.addFirst(() -> generateAndSaveIcon(baseEntry, id.getNamespace(), fileName, key, renderAction));
                    *///?}
                } else {
                    PENDING_GENERATIONS.remove(key);
                }
            }
        }, Minecraft.getInstance());

        return Optional.empty();
    }

    //? if <26.1 {
    private static void generateAndSaveIcon(String namespace, String fileName, String key, RenderAction renderAction) {
    //?} else {
    /*private static void mcRegisterWithSilhouette(String key, ResourceLocation texLoc, NativeImage image) {
        Minecraft mc = Minecraft.getInstance();

        DynamicTexture texture = new DynamicTexture(texLoc::toString, image);
        mc.getTextureManager().register(texLoc, texture);
        TEXTURE_CACHE.put(key, texLoc);

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
        ResourceLocation silLoc = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key + "_silhouette");
        mc.getTextureManager().register(silLoc, new DynamicTexture(silLoc::toString, silhouetteImage));
        TEXTURE_CACHE.put(key + "_silhouette", silLoc);
    }

    private static void generateAndSaveIcon(Object baseEntry, String namespace, String fileName, String key, RenderAction renderAction) {
    *///?}
        if (TEXTURE_CACHE.containsKey(key)) {
            PENDING_GENERATIONS.remove(key);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        //? if <26.1 {
        Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        //?}

        //? if <26.1 {
        RenderTarget renderTarget = new TextureTarget(RENDER_SIZE, RENDER_SIZE, true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        renderTarget.clear(Minecraft.ON_OSX);
        //?} else if <26.2 {
        /*RenderTarget renderTarget = new TextureTarget("IconGenerator", RENDER_SIZE, RENDER_SIZE, true);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        *///?} else if <26.3 {
        /*RenderTarget renderTarget = new TextureTarget("IconGenerator", RENDER_SIZE, RENDER_SIZE, true, GpuFormat.RGBA8_UNORM);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        *///?} else {
        /*RenderTarget renderTarget = new TextureTarget("IconGenerator", RENDER_SIZE, RENDER_SIZE, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        *///?}

        //? if <26.1 {
        renderTarget.bindWrite(true);
        RenderSystem.viewport(0, 0, RENDER_SIZE, RENDER_SIZE);
        //?} else if <26.2 {
        /*encoder.clearColorAndDepthTextures(renderTarget.getColorTexture(), 0, renderTarget.getDepthTexture(), 1.0);
        *///?} else {
        /*encoder.clearColorAndDepthTextures(renderTarget.getColorTexture(), new Vector4f(0, 0, 0, 0), renderTarget.getDepthTexture(), 0.0f);
        *///?}

        //? if <26.1 {
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Matrix4f projectionMatrix = new Matrix4f().setOrtho(0.0F, RENDER_SIZE, 0.0F, RENDER_SIZE, 1000.0F, 21000.0F);
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
        //?} else if <26.3 {
        /*RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

        RenderSystem.backupProjectionMatrix();
        *///?} else {
        /*RenderSystem.backupProjectionMatrix();
        *///?}

        //? if <26.1 {
        //? if >=1.21 {
        Matrix4fStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushMatrix();
        poseStack.identity();
        //?} else {
        /*PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.setIdentity();
        *///?}
        poseStack.translate(RENDER_SIZE / 2.0f, RENDER_SIZE / 2.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
        //?} else {
        /*Projection projection = new Projection();
        projection.setupOrtho(-1000.0F, 1000.0F, RENDER_SIZE, RENDER_SIZE, true);
        ProjectionMatrixBuffer projBuffer = new ProjectionMatrixBuffer("field_guide_icons");
        RenderSystem.setProjectionMatrix(projBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);
        *///?}

        //? if <26.1 {
        float oldFogStart = RenderSystem.getShaderFogStart();
        float oldFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        //?} else if <26.2 {
        /*PoseStack poseStack = new PoseStack();
        poseStack.translate(RENDER_SIZE / 2.0f, RENDER_SIZE / 2.0f, 0.0f);

        Object coreEntry = EntryResolver.resolveCoreEntry(baseEntry);
        boolean isEntity = coreEntry instanceof EntityType<?>;

        Vector3f light0;
        Vector3f light1;
        if (isEntity) {
            light0 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
            light1 = new Vector3f( 1.0F, -1.0F, 1.0F).normalize();
        } else {
            light0 = new Vector3f(-0.2F, -1.0F, -0.7F).normalize();
            light1 = new Vector3f( 0.2F,  0.0F,  0.7F).normalize();
        }

        GpuBuffer lightBuffer = RenderSystem.getDevice().createBuffer(() -> "FieldGuide Lighting", 136, Lighting.UBO_SIZE);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer buffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE)
                    .putVec3(light0)
                    .putVec3(light1)
                    .get();
            encoder.writeToBuffer(lightBuffer.slice(), buffer);
        }
        RenderSystem.setShaderLights(lightBuffer.slice());
        *///?} else {
        /*PoseStack poseStack = new PoseStack();
        poseStack.translate(RENDER_SIZE / 2.0f, RENDER_SIZE / 2.0f, 0.0f);

        Object coreEntry = EntryResolver.resolveCoreEntry(baseEntry);
        boolean isEntity = coreEntry instanceof EntityType<?>;

        Vector3f light0;
        Vector3f light1;
        if (isEntity) {
            light0 = new Vector3f(-1.0F, -1.0F, 1.0F).normalize();
            light1 = new Vector3f( 1.0F, -1.0F, 1.0F).normalize();
        } else {
            light0 = new Vector3f(-0.2F, -1.0F, -0.7F).normalize();
            light1 = new Vector3f( 0.2F,  0.0F,  0.7F).normalize();
        }

        GpuBuffer lightBuffer = RenderSystem.getDevice().createBuffer(() -> "FieldGuide Lighting", 136, Lighting.UBO_SIZE);

        // todo: this is also terrible
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer buffer = Std140Builder.onStack(memoryStack, Lighting.UBO_SIZE)
                    .putVec3(light0)
                    .putVec3(light1)
                    .get();
            encoder.writeToBuffer(lightBuffer.slice(), buffer);
        }
        RenderSystem.setShaderLights(lightBuffer.slice());
        *///?}

        boolean emfLoaded = Services.PLATFORM.isModLoaded("entity_model_features");
        if (emfLoaded) {
            EmfCompat.setInGui(true);
        }

        //? if >=26.1 && <26.2 {
        /*SubmitNodeStorage storage = mc.gameRenderer.getSubmitNodeStorage();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        *///?} else if >=26.2 {
        /*SubmitNodeStorage storage = new SubmitNodeStorage();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.featureRenderDispatcher();
        *///?}

        try {
            //? if <26.1 {
            renderAction.render(new PoseStack(), mc.renderBuffers().bufferSource());
            //?} else if <26.2 {
            /*renderAction.render(poseStack, storage);
            featureDispatcher.renderAllFeatures();
            buffers.endBatch();
            *///?} else if <26.3 {
            /*renderAction.render(poseStack, storage);
            featureDispatcher.renderAllFeatures(storage);
            *///?} else {
            /*renderAction.render(poseStack, storage);
            try (FeatureRenderDispatcher.PreparedFrame frame = featureDispatcher.prepareFrame(storage);
                 RenderPass renderPass = encoder.createRenderPass(() -> "FieldGuide icon", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(renderPass);
                FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
            }
            *///?}
        } finally {
            //? if >=26.1 && <26.2 {
            /*storage.clear();
            *///?}
            if (emfLoaded) {
                EmfCompat.setInGui(false);
            }
        }

        //? if <26.1 {
        RenderSystem.setShaderFogStart(oldFogStart);
        RenderSystem.setShaderFogEnd(oldFogEnd);
        //?} else {
        /*lightBuffer.close();
        RenderSystem.restoreProjectionMatrix();
        *///?}

        //? if >=1.21 && <26.1 {
        poseStack.popMatrix();
        //?} else if <1.21 {
        /*poseStack.popPose();
        *///?}
        //? if <26.1 {
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();

        RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);
        //?} else if <26.3 {
        /*RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        long bufferSize = (long) RENDER_SIZE * RENDER_SIZE * 4;
        GpuBuffer readbackBuffer = RenderSystem.getDevice().createBuffer(() -> "Readback", 9, bufferSize);
        *///?} else {
        /*long bufferSize = (long) RENDER_SIZE * RENDER_SIZE * 4;
        GpuBuffer readbackBuffer = RenderSystem.getDevice().createBuffer(() -> "Readback", 9, bufferSize);
        *///?}

        //? if <26.1 {
        renderTarget.unbindWrite();
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());

        NativeImage nativeImage = new NativeImage(RENDER_SIZE, RENDER_SIZE, false);
        //?} else if <26.2 {
        /*encoder.copyTextureToBuffer(renderTarget.getColorTexture(), readbackBuffer, 0, () -> {
            try {
                CommandEncoder mapEncoder = RenderSystem.getDevice().createCommandEncoder();

                try (GpuBuffer.MappedView view = mapEncoder.mapBuffer(readbackBuffer, true, false)) {
                    ByteBuffer pixels = view.data();
        *///?} else {
        /*encoder.copyTextureToBuffer(renderTarget.getColorTexture(), readbackBuffer, 0, () -> {
            try {
                try (GpuBufferSlice.MappedView view = readbackBuffer.map(true, false)) {
                    ByteBuffer pixels = view.data();
        *///?}

        //? if <26.1 {
        try {
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, false);
            nativeImage.flipY();
        //?} else {
                    /*NativeImage nativeImage = new NativeImage(RENDER_SIZE, RENDER_SIZE, false);
                    int bytesPerRow = RENDER_SIZE * 4;
                    long ptr = nativeImage.getPointer();
        *///?}

            //? if <26.1 {
            byte[] imageBytes = nativeImage.asByteArray();
            //?} else {
                    /*// TODO: this is terrible
                    try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                        memoryStack.malloc(bytesPerRow);
                        long srcAddr = MemoryUtil.memAddress(pixels);
            *///?}

            //? if <26.1 {
            DynamicTexture texture = new DynamicTexture(nativeImage);
            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key);
            mc.getTextureManager().register(texLoc, texture);
            TEXTURE_CACHE.put(key, texLoc);
            //?} else {
                        /*for (int i = 0; i < RENDER_SIZE; i++) {
                            long srcRowPtr = srcAddr + ((RENDER_SIZE - 1 - i) * bytesPerRow);
                            long destRowPtr = ptr + (i * bytesPerRow);
                            MemoryUtil.memCopy(srcRowPtr, destRowPtr, bytesPerRow);
                        }
                    }
            *///?}

            //? if <26.1 {
            CompletableFuture.runAsync(() -> {
                try {
                    Path cachedFilePath = CACHE_DIR.resolve(namespace).resolve("textures/fieldguide/entries").resolve(fileName);
                    Files.createDirectories(cachedFilePath.getParent());
                    Files.write(cachedFilePath, imageBytes);
                } catch (IOException e) {
                    Constants.LOG.error("Failed to save generated icon", e);
            //?} else {
                    /*NativeImage fileImage = nativeImage.mappedCopy(IntUnaryOperator.identity());

                    ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key);
                    mcRegisterWithSilhouette(key, texLoc, nativeImage);

                    CompletableFuture.runAsync(() -> {
                        try (fileImage) {
                            Path cachedFilePath = CACHE_DIR.resolve(namespace).resolve("textures/fieldguide/entries").resolve(fileName);
                            Files.createDirectories(cachedFilePath.getParent());
                            fileImage.writeToFile(cachedFilePath);
                        } catch (IOException e) {
                            Constants.LOG.error("Failed to save generated icon", e);
                        }
                    }, IO_EXECUTOR);
            *///?}
                }
        //? if <26.1 {
            }, IO_EXECUTOR);

        } catch (Exception e) {
            Constants.LOG.error("Failed to process generated icon", e);
            nativeImage.close();
        } finally {
            renderTarget.destroyBuffers();
            PENDING_GENERATIONS.remove(key);
        }
        //?} else {
            /*} catch (Exception e) {
                Constants.LOG.error("Failed to process generated icon", e);
            } finally {
                readbackBuffer.close();
                renderTarget.destroyBuffers();
                projBuffer.close();
                PENDING_GENERATIONS.remove(key);
            }
        }, 0);
        *///?}
    }
}

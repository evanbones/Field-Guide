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
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class IconCacheManager {
    private static final Path CACHE_DIR = Services.PLATFORM.getConfigDirectory().resolve("../fieldguide_cache");
    private static final int RENDER_SIZE = 256;

    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_GENERATIONS = ConcurrentHashMap.newKeySet();
    private static final Deque<Runnable> MAIN_THREAD_TASKS = new ConcurrentLinkedDeque<>();

    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(
            Math.min(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "FieldGuide-IconCache-IO");
                t.setDaemon(true);
                return t;
            }
    );

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
    }

    public static Optional<ResourceLocation> getOrGenerateIcon(Object baseEntry, Object cacheKey, boolean isPage, Runnable renderAction) {
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
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation texLoc = new ResourceLocation(Constants.MOD_ID, "generated_icon/" + key);
                Minecraft.getInstance().getTextureManager().register(texLoc, texture);
                TEXTURE_CACHE.put(key, texLoc);
                PENDING_GENERATIONS.remove(key);
            } else {
                ResourceLocation id = AutoPopulateRegistry.getEntryId(baseEntry);
                if (id != null) {
                    MAIN_THREAD_TASKS.addFirst(() -> generateAndSaveIcon(id.getNamespace(), fileName, key, renderAction));
                } else {
                    PENDING_GENERATIONS.remove(key);
                }
            }
        }, Minecraft.getInstance());

        return Optional.empty();
    }

    private static void generateAndSaveIcon(String namespace, String fileName, String key, Runnable renderAction) {
        if (TEXTURE_CACHE.containsKey(key)) {
            PENDING_GENERATIONS.remove(key);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Matrix4f oldProjection = RenderSystem.getProjectionMatrix();

        RenderTarget renderTarget = new TextureTarget(RENDER_SIZE, RENDER_SIZE, true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        renderTarget.clear(Minecraft.ON_OSX);

        renderTarget.bindWrite(true);
        RenderSystem.viewport(0, 0, RENDER_SIZE, RENDER_SIZE);

        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Matrix4f projectionMatrix = new Matrix4f().setOrtho(0.0F, RENDER_SIZE, RENDER_SIZE, 0.0F, 10000.0F, -10000.0F);
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.setIdentity();

        poseStack.translate(RENDER_SIZE / 2.0f, RENDER_SIZE / 2.0f, 1000.0f);
        RenderSystem.applyModelViewMatrix();

        boolean emfLoaded = Services.PLATFORM.isModLoaded("entity_model_features");
        if (emfLoaded) {
            EmfCompat.setInGui(true);
        }

        try {
            renderAction.run();
        } finally {
            if (emfLoaded) {
                EmfCompat.setInGui(false);
            }
        }

        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();

        RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);

        renderTarget.unbindWrite();
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());

        NativeImage nativeImage = new NativeImage(RENDER_SIZE, RENDER_SIZE, false);

        try {
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, false);
            nativeImage.flipY();

            DynamicTexture texture = new DynamicTexture(nativeImage);
            ResourceLocation texLoc = new ResourceLocation(Constants.MOD_ID, "generated_icon/" + key);
            mc.getTextureManager().register(texLoc, texture);
            TEXTURE_CACHE.put(key, texLoc);

            CompletableFuture.runAsync(() -> {
                try {
                    Path cachedFilePath = CACHE_DIR.resolve(namespace).resolve("textures/fieldguide/entries").resolve(fileName);
                    cachedFilePath.getParent().toFile().mkdirs();
                    nativeImage.writeToFile(cachedFilePath.toFile());
                } catch (IOException e) {
                    Constants.LOG.error("Failed to save generated icon", e);
                }
            }, IO_EXECUTOR);

        } catch (Exception e) {
            Constants.LOG.error("Failed to process generated icon", e);
            nativeImage.close();
        } finally {
            renderTarget.destroyBuffers();
            PENDING_GENERATIONS.remove(key);
        }
    }
}
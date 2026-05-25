package com.evandev.fieldguide.client.gui.util;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.AutoPopulateRegistry;
import com.evandev.fieldguide.compat.emf.EmfCompat;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.platform.Services;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class IconCacheManager {
    private static final Path CACHE_DIR = Services.PLATFORM.getConfigDirectory().resolve("../fieldguide_cache");
    private static final int RENDER_SIZE = 256;

    private static final Map<String, Identifier> TEXTURE_CACHE = new ConcurrentHashMap<>();
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
        for (Identifier id : TEXTURE_CACHE.values()) {
            mc.getTextureManager().release(id);
        }
        TEXTURE_CACHE.clear();
        PENDING_GENERATIONS.clear();
        MAIN_THREAD_TASKS.clear();

        CompletableFuture.runAsync(() -> {
            if (Files.exists(CACHE_DIR)) {
                try (Stream<Path> walk = Files.walk(CACHE_DIR)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    Constants.LOG.error("Failed to delete icon cache directory", e);
                }
            }
        }, IO_EXECUTOR);
    }

    public static Optional<Identifier> getOrGenerateIcon(Object baseEntry, Object cacheKey, boolean isPage, BiConsumer<PoseStack, SubmitNodeCollector> renderAction) {
        String entryKey = AutoPopulateRegistry.getEntryKey(baseEntry);
        if (entryKey.isEmpty()) return Optional.empty();

        String variantSuffix = "";
        String cacheKeyStr = cacheKey.toString();
        if (cacheKeyStr.contains("#")) {
            variantSuffix = "_" + cacheKeyStr.substring(cacheKeyStr.indexOf('#') + 1).replace(":", "_").toLowerCase(Locale.ROOT);
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
            Identifier id = AutoPopulateRegistry.getEntryId(baseEntry);
            Path cachedFilePath = CACHE_DIR.resolve(id.getNamespace()).resolve("textures/fieldguide/entries").resolve(fileName);
            File cachedFile = cachedFilePath.toFile();

            if (cachedFile.exists()) {
                try {
                    return NativeImage.read(Files.newInputStream(cachedFile.toPath()));
                } catch (IOException e) {
                    Constants.LOG.error("Failed to load cached icon: {}", key, e);
                }
            }
            return null;
        }, IO_EXECUTOR).thenAcceptAsync(image -> {
            if (image != null) {
                Identifier texLoc = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key);
                mcRegisterWithSilhouette(key, texLoc, image);
                PENDING_GENERATIONS.remove(key);
            } else {
                Identifier id = AutoPopulateRegistry.getEntryId(baseEntry);
                MAIN_THREAD_TASKS.addFirst(() -> generateAndSaveIcon(baseEntry, id.getNamespace(), fileName, key, renderAction));
            }
        }, Minecraft.getInstance());

        return Optional.empty();
    }

    private static void mcRegisterWithSilhouette(String key, Identifier texLoc, NativeImage image) {
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
        Identifier silLoc = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key + "_silhouette");
        mc.getTextureManager().register(silLoc, new DynamicTexture(silLoc::toString, silhouetteImage));
        TEXTURE_CACHE.put(key + "_silhouette", silLoc);
    }

    private static void generateAndSaveIcon(Object baseEntry, String namespace, String fileName, String key, BiConsumer<PoseStack, SubmitNodeCollector> renderAction) {
        if (TEXTURE_CACHE.containsKey(key)) {
            PENDING_GENERATIONS.remove(key);
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        RenderTarget renderTarget = new TextureTarget("IconGenerator", RENDER_SIZE, RENDER_SIZE, true);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.clearColorAndDepthTextures(renderTarget.getColorTexture(), 0, renderTarget.getDepthTexture(), 1.0);

        RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

        RenderSystem.backupProjectionMatrix();

        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, RENDER_SIZE, RENDER_SIZE, 0.0F, 1000.0F, -1000.0F);
        GpuBuffer projBuffer = RenderSystem.getDevice().createBuffer(() -> "Icon Proj", 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
            ortho.get(buffer);
            encoder.writeToBuffer(projBuffer.slice(), buffer);
        }
        RenderSystem.setProjectionMatrix(projBuffer.slice(), ProjectionType.ORTHOGRAPHIC);

        PoseStack poseStack = new PoseStack();
        poseStack.translate(RENDER_SIZE / 2.0f, RENDER_SIZE / 2.0f, 0.0f);

        Object coreEntry = EntryResolver.resolveCoreEntry(baseEntry);
        boolean isEntity = coreEntry instanceof EntityType<?>;

        Vector3f light0;
        Vector3f light1;
        if (isEntity) {
            light0 = new Vector3f(1.0F, -1.0F, -1.0F).normalize();
            light1 = new Vector3f(-1.0F, -1.0F, -1.0F).normalize();
        } else {
            light0 = new Vector3f(0.2F, -1.0F, 0.7F).normalize();
            light1 = new Vector3f(-0.2F, 0.0F, -0.7F).normalize();
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

        if (Services.PLATFORM.isModLoaded("entity_model_features")) {
            EmfCompat.setInGui(true);
        }

        SubmitNodeStorage storage = mc.gameRenderer.getSubmitNodeStorage();
        FeatureRenderDispatcher featureDispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        renderAction.accept(poseStack, storage);

        featureDispatcher.renderAllFeatures();
        buffers.endBatch();

        storage.clear();

        if (Services.PLATFORM.isModLoaded("entity_model_features")) {
            EmfCompat.setInGui(false);
        }

        lightBuffer.close();
        RenderSystem.restoreProjectionMatrix();

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        long bufferSize = (long) RENDER_SIZE * RENDER_SIZE * 4;
        GpuBuffer readbackBuffer = RenderSystem.getDevice().createBuffer(() -> "Readback", 9, bufferSize);

        encoder.copyTextureToBuffer(renderTarget.getColorTexture(), readbackBuffer, 0, () -> {
            try {
                CommandEncoder mapEncoder = RenderSystem.getDevice().createCommandEncoder();

                try (GpuBuffer.MappedView view = mapEncoder.mapBuffer(readbackBuffer, true, false)) {
                    ByteBuffer pixels = view.data();

                    NativeImage nativeImage = new NativeImage(RENDER_SIZE, RENDER_SIZE, false);
                    int bytesPerRow = RENDER_SIZE * 4;
                    long ptr = nativeImage.getPointer();

                    // TODO: this is terrible
                    try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                        memoryStack.malloc(bytesPerRow);
                        long srcAddr = MemoryUtil.memAddress(pixels);

                        for (int i = 0; i < RENDER_SIZE; i++) {
                            long srcRowPtr = srcAddr + ((RENDER_SIZE - 1 - i) * bytesPerRow);
                            long destRowPtr = ptr + (i * bytesPerRow);
                            MemoryUtil.memCopy(srcRowPtr, destRowPtr, bytesPerRow);
                        }
                    }

                    Identifier texLoc = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "generated_icon/" + key);
                    mcRegisterWithSilhouette(key, texLoc, nativeImage);

                    CompletableFuture.runAsync(() -> {
                        try {
                            Path cachedFilePath = CACHE_DIR.resolve(namespace).resolve("textures/fieldguide/entries").resolve(fileName);
                            cachedFilePath.getParent().toFile().mkdirs();
                            nativeImage.writeToFile(cachedFilePath.toFile());
                        } catch (IOException e) {
                            Constants.LOG.error("Failed to save generated icon", e);
                        }
                    }, IO_EXECUTOR);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to process generated icon", e);
            } finally {
                readbackBuffer.close();
                renderTarget.destroyBuffers();
                projBuffer.close();
                PENDING_GENERATIONS.remove(key);
            }
        }, 0);
    }
}
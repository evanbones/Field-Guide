package com.evandev.fieldguide.client;

import com.evandev.fieldguide.Constants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ModRenderTypes extends RenderType {

    private static final Map<RenderType, RenderType> SCAN_BLOCK_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> SCAN_ENTITY_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_BLOCK_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_ENTITY_CACHE = new IdentityHashMap<>();

    public static ShaderInstance SCAN_BLOCK_SHADER;
    private static final ShaderStateShard SCAN_BLOCK_STATE = new ShaderStateShard(() -> SCAN_BLOCK_SHADER);
    public static ShaderInstance SCAN_ENTITY_SHADER;
    private static final ShaderStateShard SCAN_ENTITY_STATE = new ShaderStateShard(() -> SCAN_ENTITY_SHADER);

    public ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType wrapForDepth(RenderType original, boolean isEntity) {
        try {
            Map<RenderType, RenderType> cache = isEntity ? DEPTH_ENTITY_CACHE : DEPTH_BLOCK_CACHE;
            return cache.computeIfAbsent(original, type -> new ModRenderTypes(
                    Constants.MOD_ID + "_scan_depth_wrap" + (isEntity ? "_entity" : "_block"),
                    type.format(),
                    type.mode(),
                    type.bufferSize(),
                    type.affectsCrumbling(),
                    false,
                    () -> {
                        type.setupRenderState();
                        (isEntity ? SCAN_ENTITY_STATE : SCAN_BLOCK_STATE).setupRenderState();
                        DEPTH_WRITE.setupRenderState();
                    },
                    () -> {
                        DEPTH_WRITE.clearRenderState();
                        (isEntity ? SCAN_ENTITY_STATE : SCAN_BLOCK_STATE).clearRenderState();
                        type.clearRenderState();
                    }
            ));
        } catch (Exception e) {
            return original;
        }
    }

    public static RenderType wrapForScan(RenderType original, boolean isEntity) {
        try {
            Map<RenderType, RenderType> cache = isEntity ? SCAN_ENTITY_CACHE : SCAN_BLOCK_CACHE;
            return cache.computeIfAbsent(original, type -> new ModRenderTypes(
                    Constants.MOD_ID + "_scan_wrap" + (isEntity ? "_entity" : "_block"),
                    type.format(),
                    type.mode(),
                    type.bufferSize(),
                    type.affectsCrumbling(),
                    false,
                    () -> {
                        type.setupRenderState();
                        (isEntity ? SCAN_ENTITY_STATE : SCAN_BLOCK_STATE).setupRenderState();
                        TRANSLUCENT_TRANSPARENCY.setupRenderState();
                        COLOR_WRITE.setupRenderState();
                        new DepthTestStateShard("equal_depth", GL11.GL_EQUAL).setupRenderState();
                    },
                    () -> {
                        new DepthTestStateShard("equal_depth", GL11.GL_EQUAL).clearRenderState();
                        COLOR_WRITE.clearRenderState();
                        TRANSLUCENT_TRANSPARENCY.clearRenderState();
                        (isEntity ? SCAN_ENTITY_STATE : SCAN_BLOCK_STATE).clearRenderState();
                        type.clearRenderState();
                    }
            ));
        } catch (Exception e) {
            return original;
        }
    }

    public static void registerShaders(Consumer<ShaderInstance> provider, ResourceProvider resourceProvider) throws IOException {
        provider.accept(new ShaderInstance(resourceProvider, Constants.MOD_ID + ":fieldguide_scan_block", DefaultVertexFormat.BLOCK) {
            @Override
            public void apply() {
                ModRenderTypes.SCAN_BLOCK_SHADER = this;
                super.apply();
            }
        });

        provider.accept(new ShaderInstance(resourceProvider, Constants.MOD_ID + ":fieldguide_scan_entity", DefaultVertexFormat.NEW_ENTITY) {
            @Override
            public void apply() {
                ModRenderTypes.SCAN_ENTITY_SHADER = this;
                super.apply();
            }
        });
    }
}
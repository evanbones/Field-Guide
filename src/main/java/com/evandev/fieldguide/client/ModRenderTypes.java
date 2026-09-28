package com.evandev.fieldguide.client;

//? if <26.1 {
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
//?} else {
/*import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.mixin.accessor.RenderSetupAccessor;
import com.evandev.fieldguide.mixin.accessor.RenderTypeAccessor;
import com.evandev.fieldguide.mixin.accessor.TextureBindingAccessor;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

//? if <26.3 {
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.rendertype.OutputTarget;
//?}

//? if >=26.2 && <26.3 {
/^import com.mojang.blaze3d.pipeline.BindGroupLayout;
^///?}

//? if >=26.3 {
/^import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
^///?}

public class ModRenderTypes {

    private static final Map<RenderType, RenderType> SCAN_WRAP_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_WRAP_CACHE = new IdentityHashMap<>();

    private static final ResourceLocation SCAN_SHADER_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "core/fieldguide_scan");

    // Sampler1/Sampler2 are only bound when useOverlay/useLightmap are set, and the copied bind group layouts require them
    private static RenderSetup.RenderSetupBuilder copyTextures(RenderType original, RenderSetup.RenderSetupBuilder builder) {
        RenderSetupAccessor setup = (RenderSetupAccessor) (Object) ((RenderTypeAccessor) original).fieldguide$getState();
        setup.fieldguide$getTextures().forEach((name, binding) -> {
            TextureBindingAccessor texture = (TextureBindingAccessor) binding;
            builder.withTexture(name, texture.fieldguide$getLocation(), texture.fieldguide$getSampler());
        });
        if (setup.fieldguide$useLightmap()) builder.useLightmap();
        if (setup.fieldguide$useOverlay()) builder.useOverlay();
        return builder;
    }

    public static RenderType wrapForDepth(RenderType original) {
        if (original == null) {
            return null;
        }

        try {
            return DEPTH_WRAP_CACHE.computeIfAbsent(original, type -> {
                RenderPipeline originalPipeline = type.pipeline();
                RenderPipeline translucentPipeline = RenderTypes.translucentMovingBlock().pipeline();

                RenderPipeline.Builder builder = RenderPipeline.builder()
                        .withLocation(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "scan_depth_wrap"))
                        .withVertexShader(SCAN_SHADER_ID)
                        .withFragmentShader(SCAN_SHADER_ID)
                        //? if <26.2 {
                        .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
                        .withColorTargetState(translucentPipeline.getColorTargetState())
                        .withCull(originalPipeline.isCull());
                        //?} else if <26.3 {
                        /^.withVertexBinding(0, originalPipeline.getVertexFormatBindings()[0])
                        .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                        .withColorTargetState(translucentPipeline.getColorTargetState())
                        .withCull(originalPipeline.isCull());
                        ^///?} else {
                        /^.withVertexBinding(0, originalPipeline.getVertexFormatBindings().getFirst())
                        .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                        .withColorTargetState(translucentPipeline.getColorTargetStates().getFirst())
                        .withCull(originalPipeline.isCull())
                        .withPushConstantSize(originalPipeline.pushConstantSize());
                        ^///?}

                if (originalPipeline.getDepthStencilState() != null) {
                    builder.withDepthStencilState(Optional.of(originalPipeline.getDepthStencilState()));
                }

                //? if <26.2 {
                for (String sampler : originalPipeline.getSamplers()) {
                    builder.withSampler(sampler);
                }

                for (RenderPipeline.UniformDescription uniform : originalPipeline.getUniforms()) {
                    if (uniform.textureFormat() != null) {
                        builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
                    } else {
                        builder.withUniform(uniform.name(), uniform.type());
                    }
                //?} else {
                /^for (BindGroupLayout layout : originalPipeline.getBindGroupLayouts()) {
                    builder.withBindGroupLayout(layout);
                ^///?}
                }

                RenderSetup setup = copyTextures(original, RenderSetup.builder(builder.build()))
                        //? if <26.3 {
                        .setOutputTarget(type.outputTarget())
                        //?}
                        .createRenderSetup();
                return RenderType.create(Constants.MOD_ID + "_scan_depth_wrap", setup);
            });
        } catch (Exception e) {
            return original;
        }
    }

    public static RenderType wrapForScan(RenderType original) {
        if (original == null) {
            return null;
        }

        try {
            return SCAN_WRAP_CACHE.computeIfAbsent(original, type -> {
                RenderPipeline originalPipeline = type.pipeline();
                RenderPipeline translucentPipeline = RenderTypes.translucentMovingBlock().pipeline();

                RenderPipeline.Builder builder = RenderPipeline.builder()
                        .withLocation(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "scan_wrap"))
                        .withVertexShader(SCAN_SHADER_ID)
                        .withFragmentShader(SCAN_SHADER_ID)
                        //? if <26.2 {
                        .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
                        .withColorTargetState(translucentPipeline.getColorTargetState())
                        .withCull(originalPipeline.isCull());
                        //?} else if <26.3 {
                        /^.withVertexBinding(0, originalPipeline.getVertexFormatBindings()[0])
                        .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                        .withColorTargetState(translucentPipeline.getColorTargetState())
                        .withCull(originalPipeline.isCull());
                        ^///?} else {
                        /^.withVertexBinding(0, originalPipeline.getVertexFormatBindings().getFirst())
                        .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                        .withColorTargetState(translucentPipeline.getColorTargetStates().getFirst())
                        .withCull(originalPipeline.isCull())
                        .withPushConstantSize(originalPipeline.pushConstantSize());
                        ^///?}

                builder.withDepthStencilState(Optional.of(new DepthStencilState(CompareOp.EQUAL, false)));

                //? if <26.2 {
                for (String sampler : originalPipeline.getSamplers()) {
                    builder.withSampler(sampler);
                }

                for (RenderPipeline.UniformDescription uniform : originalPipeline.getUniforms()) {
                    if (uniform.textureFormat() != null) {
                        builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
                    } else {
                        builder.withUniform(uniform.name(), uniform.type());
                    }
                //?} else {
                /^for (BindGroupLayout layout : originalPipeline.getBindGroupLayouts()) {
                    builder.withBindGroupLayout(layout);
                ^///?}
                }

                RenderSetup setup = copyTextures(original, RenderSetup.builder(builder.build()))
                        //? if <26.3 {
                        .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                        //?}
                        .sortOnUpload()
                        .createRenderSetup();
                return RenderType.create(Constants.MOD_ID + "_scan_wrap", setup);
            });
        } catch (Exception e) {
            return original;
        }
    }
}
*///?}

package com.evandev.fieldguide.client;

import com.evandev.fieldguide.Constants;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public class ModRenderTypes {

    private static final Map<RenderType, RenderType> SCAN_WRAP_CACHE = new IdentityHashMap<>();
    private static final Map<RenderType, RenderType> DEPTH_WRAP_CACHE = new IdentityHashMap<>();

    private static final Identifier SCAN_SHADER_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "core/fieldguide_scan");

    private static void injectTextures(RenderType original, RenderSetup newSetup) {
        try {
            Field stateField = null;
            for (Field f : RenderType.class.getDeclaredFields()) {
                if (f.getType() == RenderSetup.class) {
                    f.setAccessible(true);
                    stateField = f;
                    break;
                }
            }
            if (stateField == null) return;
            RenderSetup originalSetup = (RenderSetup) stateField.get(original);

            for (Field f : RenderSetup.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(newSetup, f.get(originalSetup));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RenderType wrapForDepth(RenderType original) {
        if (original == null) {
            return null;
        }

        return DEPTH_WRAP_CACHE.computeIfAbsent(original, type -> {
            RenderPipeline originalPipeline = type.pipeline();
            RenderPipeline translucentPipeline = RenderTypes.translucentMovingBlock().pipeline();

            RenderPipeline.Builder builder = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "scan_depth_wrap"))
                    .withVertexShader(SCAN_SHADER_ID)
                    .withFragmentShader(SCAN_SHADER_ID)
                    .withVertexBinding(0, originalPipeline.getVertexFormatBindings()[0])
                    .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                    .withColorTargetState(translucentPipeline.getColorTargetState())
                    .withCull(originalPipeline.isCull());

            if (originalPipeline.getDepthStencilState() != null) {
                builder.withDepthStencilState(Optional.of(originalPipeline.getDepthStencilState()));
            }

            for (BindGroupLayout layout : originalPipeline.getBindGroupLayouts()) {
                builder.withBindGroupLayout(layout);
            }

            RenderSetup setup = RenderSetup.builder(builder.build())
                    .setOutputTarget(type.outputTarget())
                    .createRenderSetup();

            injectTextures(original, setup);
            return RenderType.create(Constants.MOD_ID + "_scan_depth_wrap", setup);
        });
    }

    public static RenderType wrapForScan(RenderType original) {
        if (original == null) {
            return null;
        }

        return SCAN_WRAP_CACHE.computeIfAbsent(original, type -> {
            RenderPipeline originalPipeline = type.pipeline();
            RenderPipeline translucentPipeline = RenderTypes.translucentMovingBlock().pipeline();

            RenderPipeline.Builder builder = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "scan_wrap"))
                    .withVertexShader(SCAN_SHADER_ID)
                    .withFragmentShader(SCAN_SHADER_ID)
                    .withVertexBinding(0, originalPipeline.getVertexFormatBindings()[0])
                    .withPrimitiveTopology(originalPipeline.getPrimitiveTopology())
                    .withColorTargetState(translucentPipeline.getColorTargetState())
                    .withCull(originalPipeline.isCull());

            builder.withDepthStencilState(Optional.of(new DepthStencilState(CompareOp.EQUAL, false)));

            for (BindGroupLayout layout : originalPipeline.getBindGroupLayouts()) {
                builder.withBindGroupLayout(layout);
            }

            RenderSetup setup = RenderSetup.builder(builder.build())
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .sortOnUpload()
                    .createRenderSetup();

            injectTextures(original, setup);
            return RenderType.create(Constants.MOD_ID + "_scan_wrap", setup);
        });
    }
}
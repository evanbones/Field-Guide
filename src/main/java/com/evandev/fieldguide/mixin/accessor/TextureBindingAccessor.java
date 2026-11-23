package com.evandev.fieldguide.mixin.accessor;

//? if >=26.1 {
/*import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

//? if <26.3 {
import com.mojang.blaze3d.textures.GpuSampler;
//?} else {
/^import com.mojang.renderpearl.api.textures.GpuSampler;
^///?}

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding", remap = false)
public interface TextureBindingAccessor {
    @Accessor("location")
    ResourceLocation fieldguide$getLocation();

    @Accessor("sampler")
    Supplier<GpuSampler> fieldguide$getSampler();
}
*///?}

package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1 {
/*import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;
*///?}

//? if >=26.1 && <26.3 {
/*import com.mojang.blaze3d.textures.GpuSampler;
*///?} else if >=26.3 {
/*import com.mojang.renderpearl.api.textures.GpuSampler;
*///?}

@IfMinecraftVersion(minVersion = "26.1")
//? if <1.21 {
/*@Mixin(targets = "net.minecraft.client.renderer.RenderType", remap = false)
*///?} else {
@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding", remap = false)
//?}
public interface TextureBindingAccessor {
    //? if >=26.1 {
    /*@Accessor("location")
    ResourceLocation fieldguide$getLocation();

    @Accessor("sampler")
    Supplier<GpuSampler> fieldguide$getSampler();
    *///?}
}

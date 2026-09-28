package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
*///?}

@IfMinecraftVersion(minVersion = "26.1")
//? if <26.1 {
@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup", remap = false)
//?} else {
/*@Mixin(RenderSetup.class)
*///?}
public interface RenderSetupAccessor {
    //? if >=26.1 {
    /*// Values are RenderSetup.TextureBinding, which is package-private; read them via TextureBindingAccessor
    @Accessor("textures")
    Map<String, ?> fieldguide$getTextures();

    @Accessor("useLightmap")
    boolean fieldguide$useLightmap();

    @Accessor("useOverlay")
    boolean fieldguide$useOverlay();
    *///?}
}

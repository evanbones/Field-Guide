package com.evandev.fieldguide.mixin.accessor;

//? if >=26.1 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    // Values are RenderSetup.TextureBinding, which is package-private; read them via TextureBindingAccessor
    @Accessor("textures")
    Map<String, ?> fieldguide$getTextures();

    @Accessor("useLightmap")
    boolean fieldguide$useLightmap();

    @Accessor("useOverlay")
    boolean fieldguide$useOverlay();
}
*///?}

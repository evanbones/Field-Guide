package com.evandev.fieldguide.mixin.accessor;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.1 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

@IfMinecraftVersion(minVersion = "26.1")
//? if <26.1 {
@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderType", remap = false)
//?} else {
/*@Mixin(RenderType.class)
*///?}
public interface RenderTypeAccessor {
    //? if >=26.1 {
    /*@Accessor("state")
    RenderSetup fieldguide$getState();
    *///?}
}

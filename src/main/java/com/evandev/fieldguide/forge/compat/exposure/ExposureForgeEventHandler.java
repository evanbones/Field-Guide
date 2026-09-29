package com.evandev.fieldguide.forge.compat.exposure;

//? if forge {
/*import com.evandev.fieldguide.compat.exposure.ExposureCompat;
import io.github.mortuusars.exposure.forge.api.event.FrameAddedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ExposureForgeEventHandler {

    @SubscribeEvent
    public static void onFrameAdded(FrameAddedEvent event) {
        if (event.getCameraHolder().asHolderEntity() instanceof Player player) {
            ExposureCompat.onPhotographTaken(player, event.getFrame());
        }
    }
}
*///?}

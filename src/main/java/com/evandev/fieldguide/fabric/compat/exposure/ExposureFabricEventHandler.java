package com.evandev.fieldguide.fabric.compat.exposure;

//? if <26.1 {
//? if fabric {
/*import com.evandev.fieldguide.compat.exposure.ExposureCompat;
import io.github.mortuusars.exposure.fabric.api.event.FrameAddedCallback;
import net.minecraft.world.entity.player.Player;

public class ExposureFabricEventHandler {
    public static void register() {
        FrameAddedCallback.EVENT.register((cameraHolder, camera, frame, positionsInFrame, entitiesInFrame) -> {
            if (cameraHolder.asHolderEntity() instanceof Player player) {
                ExposureCompat.onPhotographTaken(player, frame);
            }
        });
    }
}
*///?}
//?}

package com.evandev.fieldguide.client.scan;

import com.evandev.fieldguide.client.scan.manager.FieldGuideScanManager;
import com.evandev.fieldguide.client.scan.manager.FieldGuideScanState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class FieldGuideScanner {
    private static final FieldGuideScanner INSTANCE = new FieldGuideScanner();

    private FieldGuideScanner() {
    }

    public static FieldGuideScanner getInstance() {
        return INSTANCE;
    }

    public void onClientTick(Minecraft minecraft) {
        FieldGuideScanManager.getInstance().onClientTick(minecraft);
    }

    public Object getScanningTarget() {
        return FieldGuideScanState.getInstance().getScanningTarget();
    }

    public BlockPos getScanningPos() {
        return FieldGuideScanState.getInstance().getScanningPos();
    }

    public Entity getScanningEntity() {
        return FieldGuideScanState.getInstance().getScanningEntity();
    }

    public float getScanProgress(float partialTicks) {
        return FieldGuideScanManager.getInstance().getScanProgress(partialTicks);
    }

    public Object getOutOfRangeTarget() {
        return FieldGuideScanState.getInstance().getOutOfRangeTarget();
    }

    public BlockPos getOutOfRangePos() {
        return FieldGuideScanState.getInstance().getOutOfRangePos();
    }

    public Entity getOutOfRangeEntity() {
        return FieldGuideScanState.getInstance().getOutOfRangeEntity();
    }

    public Object getFadingTarget() {
        return FieldGuideScanState.getInstance().getFadingTarget();
    }

    public BlockPos getFadingPos() {
        return FieldGuideScanState.getInstance().getFadingPos();
    }

    public Entity getFadingEntity() {
        return FieldGuideScanState.getInstance().getFadingEntity();
    }

    public float getFadeProgress(float partialTicks) {
        return FieldGuideScanManager.getInstance().getFadeProgress(partialTicks);
    }

    public boolean getIsTickingDown() {
        FieldGuideScanState state = FieldGuideScanState.getInstance();
        return state.getScanTicks() < state.getPrevScanTicks();
    }
}

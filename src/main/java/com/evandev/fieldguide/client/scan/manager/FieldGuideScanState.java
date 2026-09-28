package com.evandev.fieldguide.client.scan.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class FieldGuideScanState {
    private static final FieldGuideScanState INSTANCE = new FieldGuideScanState();

    private Object scanningTarget = null;
    private Object scanningEntry = null;
    private int scanTicks = 0;
    private int prevScanTicks = 0;
    private BlockPos scanningPos = null;

    private Object fadingTarget = null;
    private Object fadingEntry = null;
    private int fadeTicks = 0;
    private BlockPos fadingPos = null;

    private Object outOfRangeTarget = null;
    private BlockPos outOfRangePos = null;

    private BlockPos lastDisambiguatedPos = null;
    private Object lastDisambiguatedEntry = null;

    private FieldGuideScanState() {
    }

    public static FieldGuideScanState getInstance() {
        return INSTANCE;
    }

    public void resetScanState() {
        resetScanTicks();
        outOfRangeTarget = null;
        outOfRangePos = null;
    }

    public void resetScanTicks() {
        scanningTarget = null;
        scanningEntry = null;
        scanningPos = null;
        scanTicks = 0;
        prevScanTicks = 0;
    }

    public Object getScanningTarget() {
        return scanningTarget;
    }

    public void setScanningTarget(Object scanningTarget) {
        this.scanningTarget = scanningTarget;
    }

    public Object getScanningEntry() {
        return scanningEntry;
    }

    public void setScanningEntry(Object scanningEntry) {
        this.scanningEntry = scanningEntry;
    }

    public int getScanTicks() {
        return scanTicks;
    }

    public void setScanTicks(int scanTicks) {
        this.scanTicks = scanTicks;
    }

    public int getPrevScanTicks() {
        return prevScanTicks;
    }

    public void setPrevScanTicks(int prevScanTicks) {
        this.prevScanTicks = prevScanTicks;
    }

    public BlockPos getScanningPos() {
        return scanningPos;
    }

    public void setScanningPos(BlockPos scanningPos) {
        this.scanningPos = scanningPos;
    }

    public Object getFadingTarget() {
        return fadingTarget;
    }

    public void setFadingTarget(Object fadingTarget) {
        this.fadingTarget = fadingTarget;
    }

    public Object getFadingEntry() {
        return fadingEntry;
    }

    public void setFadingEntry(Object fadingEntry) {
        this.fadingEntry = fadingEntry;
    }

    public int getFadeTicks() {
        return fadeTicks;
    }

    public void setFadeTicks(int fadeTicks) {
        this.fadeTicks = fadeTicks;
    }

    public BlockPos getFadingPos() {
        return fadingPos;
    }

    public void setFadingPos(BlockPos fadingPos) {
        this.fadingPos = fadingPos;
    }

    public Object getOutOfRangeTarget() {
        return outOfRangeTarget;
    }

    public void setOutOfRangeTarget(Object outOfRangeTarget) {
        this.outOfRangeTarget = outOfRangeTarget;
    }

    public BlockPos getOutOfRangePos() {
        return outOfRangePos;
    }

    public void setOutOfRangePos(BlockPos outOfRangePos) {
        this.outOfRangePos = outOfRangePos;
    }

    public BlockPos getLastDisambiguatedPos() {
        return lastDisambiguatedPos;
    }

    public void setLastDisambiguatedPos(BlockPos lastDisambiguatedPos) {
        this.lastDisambiguatedPos = lastDisambiguatedPos;
    }

    public Object getLastDisambiguatedEntry() {
        return lastDisambiguatedEntry;
    }

    public void setLastDisambiguatedEntry(Object lastDisambiguatedEntry) {
        this.lastDisambiguatedEntry = lastDisambiguatedEntry;
    }

    public Entity getScanningEntity() {
        return scanningTarget instanceof Entity ? (Entity) scanningTarget : null;
    }

    public Entity getOutOfRangeEntity() {
        return outOfRangeTarget instanceof Entity ? (Entity) outOfRangeTarget : null;
    }

    public Entity getFadingEntity() {
        return fadingTarget instanceof Entity ? (Entity) fadingTarget : null;
    }
}

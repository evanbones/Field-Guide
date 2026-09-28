package com.evandev.fieldguide.client.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class EntryVisual {
    // Global Defaults
    public float scale = 1.0f;
    public float yOffset = 0.0f;
    public float xOffset = 0.0f;
    public List<ResourceLocation> spawnBiomes = null;

    // Custom Sounds & Icons
    public ResourceLocation customSound = null;
    public ResourceLocation alignmentIcon = null;

    // Grid Overrides
    public Float gridScale = null;
    public Float gridYOffset = null;
    public Float gridXOffset = null;

    // Page Overrides
    public Float pageScale = null;
    public Float pageYOffset = null;
    public Float pageXOffset = null;
}

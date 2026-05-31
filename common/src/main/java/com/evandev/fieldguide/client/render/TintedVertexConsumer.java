package com.evandev.fieldguide.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class TintedVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float tintR, tintG, tintB, tintA;
    private boolean errored = false;

    public TintedVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) {
        this.delegate = delegate;
        this.tintR = r;
        this.tintG = g;
        this.tintB = b;
        this.tintA = a;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        if (errored) return this;
        try {
            delegate.addVertex(x, y, z);
        } catch (IllegalStateException e) {
            errored = true;
        }
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha) {
        if (errored) return this;
        delegate.setColor(
                (int) (255 * tintR),
                (int) (255 * tintG),
                (int) (255 * tintB),
                (int) (alpha * tintA)
        );
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        if (errored) return this;
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int i, int i1) {
        if (errored) return this;
        delegate.setUv1(i, i1);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int i, int i1) {
        if (errored) return this;
        delegate.setUv2(i, i1);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        if (errored) return this;
        delegate.setNormal(x, y, z);
        return this;
    }
}
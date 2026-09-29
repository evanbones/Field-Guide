package com.evandev.fieldguide.client.render;

//? if <26.1 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class NoopVertexConsumer implements VertexConsumer {
    //? if >=1.21 {
    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }
    //?} else {
    /*@Override
    public @NotNull VertexConsumer vertex(double x, double y, double z) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer color(int r, int g, int b, int a) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer uv(float u, float v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public @NotNull VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void endVertex() {
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
    }

    @Override
    public void unsetDefaultColor() {
    }
    *///?}
}
//?}

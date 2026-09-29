package com.evandev.fieldguide.client.gui.util.legacy;

//? if <1.21 {
/*import com.evandev.fieldguide.Constants;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LegacySprites {
    private static final Map<ResourceLocation, Sprite> CACHE = new HashMap<>();
    private static final MetadataSectionSerializer<NineSlice> GUI_SECTION = new MetadataSectionSerializer<>() {
        @Override
        public String getMetadataSectionName() {
            return "gui";
        }

        @Override
        public NineSlice fromJson(JsonObject json) {
            JsonObject scaling = GsonHelper.getAsJsonObject(json, "scaling", new JsonObject());
            if (!"nine_slice".equals(GsonHelper.getAsString(scaling, "type", ""))) return null;

            int left, top, right, bottom;
            if (scaling.get("border") != null && scaling.get("border").isJsonObject()) {
                JsonObject border = scaling.getAsJsonObject("border");
                left = GsonHelper.getAsInt(border, "left");
                top = GsonHelper.getAsInt(border, "top");
                right = GsonHelper.getAsInt(border, "right");
                bottom = GsonHelper.getAsInt(border, "bottom");
            } else {
                left = top = right = bottom = GsonHelper.getAsInt(scaling, "border");
            }
            return new NineSlice(left, top, right, bottom);
        }
    };

    private LegacySprites() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void blit(GuiGraphics g, ResourceLocation sprite, int x, int y, int width, int height) {
        Sprite info = CACHE.computeIfAbsent(sprite, LegacySprites::load);
        if (info.slice == null) {
            g.blit(info.texture, x, y, width, height, 0, 0, info.width, info.height, info.width, info.height);
            return;
        }

        NineSlice s = info.slice;
        int left = Math.min(s.left, width / 2), right = Math.min(s.right, width / 2);
        int top = Math.min(s.top, height / 2), bottom = Math.min(s.bottom, height / 2);
        int innerW = width - left - right, innerH = height - top - bottom;
        int srcInnerW = info.width - s.left - s.right, srcInnerH = info.height - s.top - s.bottom;

        int[] dx = {x, x + left, x + width - right};
        int[] dw = {left, innerW, right};
        int[] su = {0, s.left, info.width - s.right};
        int[] sw = {s.left, srcInnerW, s.right};
        int[] dy = {y, y + top, y + height - bottom};
        int[] dh = {top, innerH, bottom};
        int[] sv = {0, s.top, info.height - s.bottom};
        int[] sh = {s.top, srcInnerH, s.bottom};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dw[col] <= 0 || dh[row] <= 0 || sw[col] <= 0 || sh[row] <= 0) continue;
                g.blit(info.texture, dx[col], dy[row], dw[col], dh[row], su[col], sv[row], sw[col], sh[row], info.width, info.height);
            }
        }
    }

    private static Sprite load(ResourceLocation sprite) {
        ResourceLocation texture = sprite.withPath(path -> "textures/gui/sprites/" + path + ".png");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return new Sprite(texture, 16, 16, null);

        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            NineSlice slice = resource.get().metadata().getSection(GUI_SECTION).orElse(null);
            return new Sprite(texture, image.getWidth(), image.getHeight(), slice);
        } catch (Exception e) {
            Constants.LOG.warn("Failed to read GUI sprite {}", texture, e);
            return new Sprite(texture, 16, 16, null);
        }
    }

    private record Sprite(ResourceLocation texture, int width, int height, NineSlice slice) {
    }

    private record NineSlice(int left, int top, int right, int bottom) {
    }
}
*///?}

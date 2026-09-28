package com.evandev.fieldguide.api.attribute;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AttributeRegistry {
    private static final List<AttributeProvider> PROVIDERS = new ArrayList<>();

    public static void register(AttributeProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<GuideAttribute> getAttributes(Object entry, @Nullable Entity renderedEntity) {
        List<GuideAttribute> attributes = new ArrayList<>();
        for (AttributeProvider provider : PROVIDERS) {
            attributes.addAll(provider.getAttributes(entry, renderedEntity));
        }
        return attributes;
    }
}

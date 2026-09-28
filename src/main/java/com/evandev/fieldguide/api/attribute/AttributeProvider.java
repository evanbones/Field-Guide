package com.evandev.fieldguide.api.attribute;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AttributeProvider {
    List<GuideAttribute> getAttributes(Object entry, @Nullable Entity renderedEntity);
}

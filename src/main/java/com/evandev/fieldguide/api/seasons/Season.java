package com.evandev.fieldguide.api.seasons;

import net.minecraft.network.chat.Component;

public enum Season {
    SPRING("spring", "fieldguide.season.spring"),
    SUMMER("summer", "fieldguide.season.summer"),
    AUTUMN("autumn", "fieldguide.season.autumn"),
    WINTER("winter", "fieldguide.season.winter");

    private final String id;
    private final String translationKey;

    Season(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }
}

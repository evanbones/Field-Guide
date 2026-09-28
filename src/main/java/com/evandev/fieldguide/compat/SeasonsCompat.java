package com.evandev.fieldguide.compat;

import com.evandev.fieldguide.api.seasons.SeasonsAPI;
import com.evandev.fieldguide.compat.eclipticseasons.EclipticSeasonsProvider;
import com.evandev.fieldguide.compat.sereneseasons.SereneSeasonsProvider;
import com.evandev.fieldguide.platform.Services;

public class SeasonsCompat {
    public static void init() {
        if (Services.PLATFORM.isModLoaded("sereneseasons")) {
            SeasonsAPI.registerProvider(new SereneSeasonsProvider());
        }

        if (Services.PLATFORM.isModLoaded("eclipticseasons")) {
            SeasonsAPI.registerProvider(new EclipticSeasonsProvider());
        }
    }
}

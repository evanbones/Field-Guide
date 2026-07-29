package com.evandev.fieldguide.compat;

import com.evandev.fieldguide.config.ModConfigScreen;
import com.evandev.fieldguide.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            return ModConfigScreen::createScreen;
        }
        return _ -> null;
    }
}
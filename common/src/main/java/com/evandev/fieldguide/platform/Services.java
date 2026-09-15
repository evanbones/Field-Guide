package com.evandev.fieldguide.platform;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.platform.services.IClientHelper;
import com.evandev.fieldguide.platform.services.INetworkHelper;
import com.evandev.fieldguide.platform.services.IPlatformHelper;
import com.evandev.fieldguide.platform.services.IRegistryHelper;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final IClientHelper CLIENT = loadOptional(IClientHelper.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static <T> T loadOptional(Class<T> clazz) {
        return ServiceLoader.load(clazz).findFirst().orElse(null);
    }
}

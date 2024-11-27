package dev.upcraft.libraryinjector.api.util;

import java.util.ServiceLoader;

public final class Environment {

    private Environment() {
        throw new UnsupportedOperationException();
    }

    public static final boolean FORCE_RELAUNCH_ON_POSIX_SYSTEMS = Boolean.getBoolean("injector.relauncher.force_relaunch_on_posix_systems");

    public static <T> T loadService(Class<T> service) {
        return ServiceLoader.load(service).findFirst().orElseThrow(() -> new IllegalStateException("No service implementation provided for " + service.getName()));
    }
}

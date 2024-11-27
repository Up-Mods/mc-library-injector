package dev.upcraft.libraryinjector.api.util;

public final class Environment {

    private Environment() {
        throw new UnsupportedOperationException();
    }

    public static final boolean FORCE_RELAUNCH_ON_POSIX_SYSTEMS = Boolean.getBoolean("injector.relauncher.force_relaunch_on_posix_systems");
}

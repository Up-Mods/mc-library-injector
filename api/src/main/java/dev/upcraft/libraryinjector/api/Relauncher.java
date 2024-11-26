package dev.upcraft.libraryinjector.api;

import dev.upcraft.libraryinjector.MinecraftFabricRelauncher;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

public final class Relauncher {

    private Relauncher() {
        throw new UnsupportedOperationException();
    }

    public static void relaunch(boolean load, BooleanSupplier abortGetter, Path... jars) {
        MinecraftFabricRelauncher.relaunch(load, abortGetter, jars);
    }
}

package dev.upcraft.libraryinjector;

import com.sun.jna.Native;
import dev.upcraft.libraryinjector.api.util.Environment;
import dev.upcraft.libraryinjector.api.util.OperatingSystem;
import dev.upcraft.libraryinjector.natives.linux.LibC;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class MinecraftFabricRelauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftFabricRelauncher.class);

    public static void relaunch(boolean load, BooleanSupplier abortGetter, Path... toAdd) {
        LOGGER.debug("Relaunching Minecraft");
        List<String> argList = new LinkedList<>();

        // java binary
        argList.add(ProcessHandle.current().info().command().orElseThrow(() -> new IllegalStateException("Failed to get java executable path")));

        // vm args
        argList.addAll(getInputArgs());

        // classpath
        argList.add("-cp");

        var originalClasspath = System.getProperty("java.class.path");
        var splitClasspath = new ArrayList<>(Arrays.stream(originalClasspath.split(File.pathSeparator)).filter(s -> !s.isBlank()).map(Path::of).toList());
        if (!load) {
            var toRemove = Arrays.stream(toAdd).map(MinecraftFabricRelauncher::tryMapToAbsolute).toList();
            splitClasspath.removeIf(p -> toRemove.contains(tryMapToAbsolute(p)));
        }
        else {
            var absolutes = splitClasspath.stream().map(MinecraftFabricRelauncher::tryMapToAbsolute).toList();
            for (Path path : toAdd) {
                var toAddAbsolute = tryMapToAbsolute(path);
                if(!absolutes.contains(toAddAbsolute)) {
                    splitClasspath.add(path);
                }
            }
        }
        argList.add(splitClasspath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));

        // main args
        argList.addAll(getMainArgs());

        // quote parameters where necessary
        argList.replaceAll(s -> {
            if (s.matches("\\w") && !s.startsWith("\"")) {
                return "\"" + s + "\"";
            } else {
                return s;
            }
        });

        if (abortGetter.getAsBoolean()) {
            LOGGER.info("Launch override detected, rejecting relaunch");
            return;
        }

        // use libc execvp on linux to avoid server hosts treating the VM as stopped
        if (!Environment.FORCE_RELAUNCH_ON_POSIX_SYSTEMS && OperatingSystem.current() == OperatingSystem.LINUX) {
            try {
                var executable = argList.get(0);
                if (executable.indexOf(File.separatorChar) != -1) {
                    argList.set(0, Path.of(executable).getFileName().toString());
                }
                if (LibC.INSTANCE.execvp(executable, argList.toArray(String[]::new)) == -1) {
                    var errorCode = Native.getLastError();
                    LOGGER.error("Failed to execvp: got unexpected error code {}. Trying to relaunch via new process...", errorCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("unable to relaunch Minecraft", e);
            }
        }

        var pb = new ProcessBuilder(argList).inheritIO();
        var hook = new Thread(() -> {
            try {
                pb.start();
            } catch (IOException e) {
                throw new RuntimeException("unable to relaunch Minecraft", e);
            }
        });

        Runtime.getRuntime().addShutdownHook(hook);

        System.exit(0);
    }

    private static Path tryMapToAbsolute(Path in) {
        try {
            return in.toAbsolutePath();
        } catch (IOError error) {
            LOGGER.debug("unable to resolve path {}", in, error);
            return in;
        }
    }

    private static List<String> getInputArgs() {
        var inputArgs = new LinkedList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());

        // need to special-case macOS
        if (OperatingSystem.current().isMacOS() && !inputArgs.contains("-XstartOnFirstThread")) {

            // find the first argument that isn't an -XX argument and add -XstartOnFirstThread
            for (int i = 0; i < inputArgs.size() + 1; i++) {
                if (i < inputArgs.size() && inputArgs.get(i).startsWith("-XX")) {
                    continue;
                }

                inputArgs.add(i, "-XstartOnFirstThread");
                break;
            }
        }

        return inputArgs;
    }

    private static List<String> getMainArgs() {
        List<String> mainArgs = new LinkedList<>();
        String mainClass;
        boolean quilt = FabricLoader.getInstance().isModLoaded("quilt_loader");
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            var useFabric = true;
            if (quilt) {
                try {
                    Class.forName("org.quiltmc.devlaunchinjector.Main");
                    useFabric = false;
                } catch (ClassNotFoundException ignored) {
                }
            }
            mainClass = useFabric ? "net.fabricmc.devlaunchinjector.Main" : "org.quiltmc.devlaunchinjector.Main";
        } else {
            if (quilt) {
                mainClass = switch (FabricLoader.getInstance().getEnvironmentType()) {
                    case CLIENT -> "org.quiltmc.loader.impl.launch.knot.KnotClient";
                    case SERVER -> "org.quiltmc.loader.impl.launch.server.QuiltServerLauncher";
                };
            } else {
                mainClass = switch (FabricLoader.getInstance().getEnvironmentType()) {
                    case CLIENT -> "net.fabricmc.loader.impl.launch.knot.KnotClient";
                    case SERVER -> {
                        // Fabric server might be using the installer, so we need to check for that
                        try {
                            var serverLauncher = "net.fabricmc.installer.ServerLauncher";
                            Class.forName(serverLauncher);
                            yield serverLauncher;
                        } catch (ClassNotFoundException e) {
                            yield "net.fabricmc.loader.impl.launch.server.FabricServerLauncher";
                        }
                    }
                };
            }
        }
        mainArgs.add(mainClass);
        String[] launchArgs = FabricLoader.getInstance().getLaunchArguments(false);
        mainArgs.addAll(Arrays.asList(launchArgs));
        return mainArgs;
    }
}

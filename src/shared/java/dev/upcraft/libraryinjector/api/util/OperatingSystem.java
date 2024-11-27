package dev.upcraft.libraryinjector.api.util;


import java.util.Locale;

public enum OperatingSystem {
    LINUX,
    SOLARIS,
    WINDOWS,
    OSX,
    UNKNOWN;

    private static OperatingSystem parseOsString() {
        var name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (name.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (name.contains("mac")) {
            return OperatingSystem.OSX;
        } else if (name.contains("solaris")) {
            return OperatingSystem.SOLARIS;
        } else if (name.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else if (name.contains("linux")) {
            return OperatingSystem.LINUX;
        } else {
            return name.contains("unix") ? OperatingSystem.LINUX : OperatingSystem.UNKNOWN;
        }
    }

    private static final OperatingSystem CURRENT = parseOsString();

    public static OperatingSystem current() {
        return CURRENT;
    }

    public boolean isPosixCompliant() {
        return this == LINUX || this == OSX;
    }

    public boolean isMacOS() {
        return this == OSX;
    }
}

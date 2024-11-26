package dev.upcraft.libraryinjector.natives.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibC extends Library {

    LibC INSTANCE = Native.load("c", LibC.class);

    int execvp(String path, String[] args);
}

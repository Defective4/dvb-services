package io.github.defective4.tv.dvbservices.util;

import java.io.IOException;

public class ProcessUtils {
    private ProcessUtils() {}

    public static Process start(String... args) throws IOException {
        Process process = new ProcessBuilder(args).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
        return process;
    }
}

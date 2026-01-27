package io.github.defective4.tv.dvbservices.ts.test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;

public class TestTSProvider implements TransportStreamProvider {
    private static final String TSP_EXECUTABLE = "tsp";

    private Process process;

    @Override
    public void close() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    @Override
    public void dumpPSI(int frequency, File output, long timeout) throws IOException {
        checkUsed();
        String[] args = { TSP_EXECUTABLE, "-I", "file", "/tmp/tv.ts", "-P", "filter", "--psi-si", "-O", "file",
                output.getPath() };
        process = new ProcessBuilder(args).start();
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
    }

    private void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

}

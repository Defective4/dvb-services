package io.github.defective4.tv.dvbservices.ts.test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;

public class TestTSProvider extends TransportStreamProvider {

    private static final String TSP_EXECUTABLE = "tsp";

    private Process process;

    private TestTSProvider() {}

    @Override
    public void close() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    @Override
    public void dumpPSI(int frequency, File output, File patOutput, File sdtOutput, long timeout) throws IOException {
        checkUsed();
        String[] args = { TSP_EXECUTABLE, "-I", "file", "/tmp/tv.ts", "-P", "filter", "--psi-si", "-P", "tables", "-p",
                "0", "--xml-output", patOutput.getPath(), "-P", "tables", "-p", "17", "--xml-output",
                sdtOutput.getPath(), "-O", "file", output.getPath() };
        process = new ProcessBuilder(args).start();
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
    }

    private void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    public static TransportStreamProviderFactory<TestTSProvider> factory() {
        return new TransportStreamProviderFactory<>() {

            @Override
            public TestTSProvider create() {
                return new TestTSProvider();
            }
        };
    }

}

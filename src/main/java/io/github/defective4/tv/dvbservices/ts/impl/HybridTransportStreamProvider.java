package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;

public class HybridTransportStreamProvider extends TSDuckProvider {

    private Process process;
    private final String tspPath;
    private final String vlcPath;

    private HybridTransportStreamProvider(String vlcPath, String tspPath) {
        super(tspPath);
        this.vlcPath = vlcPath;
        this.tspPath = tspPath;
    }

    @Override
    public InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException {
        checkUsed();
        String input = adapter.input();
        if (input == null) throw new IllegalArgumentException("input can't be null when using hybrid adapter");
        process = ProcessUtils.start(vlcPath, "-I", "dummy", "--sout", "#file{dst=/dev/stdout,mux=ts}", "--no-sout-all",
                "--sout-keep", adapter.input(), ":program=" + service);
        return process.getInputStream();
    }

    @Override
    public void close() {
        super.close();
        if (process != null) process.destroyForcibly();
    }

    @Override
    public String getFullName() {
        return String.format("Hybrid (%s + %s)", vlcPath, tspPath);
    }

    @Override
    public boolean isAvailable() throws IOException {
        if (!super.isAvailable()) return false;
        Process proc = ProcessUtils.start(vlcPath, "--version");
        try (BufferedReader reader = proc.errorReader()) {
            String line = reader.readLine();
            return line == null;
        }
    }

    @Override
    protected void checkUsed() {
        super.checkUsed();
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    public static TransportStreamProviderFactory<HybridTransportStreamProvider> factory(String vlcPath,
            String tspPath) {
        return () -> new HybridTransportStreamProvider(vlcPath, tspPath);
    }

}

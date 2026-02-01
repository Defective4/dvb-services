package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.ts.ProviderFactory;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;

public class VLCTransportStreamProvider implements TransportStreamProvider {

    private Process process;
    private final String vlcPath;

    private VLCTransportStreamProvider(String vlcPath) {
        this.vlcPath = vlcPath;
    }

    @Override
    public InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException {
        checkUsed();
        String input = adapter.streamOptions().input();
        if (input == null) throw new IllegalArgumentException("input can't be null when using hybrid adapter");
        process = ProcessUtils.start(vlcPath, "-I", "dummy", "--sout", "#file{dst=/dev/stdout,mux=ts}", "--no-sout-all",
                "--sout-keep", adapter.streamOptions().input(), ":program=" + service);
        return process.getInputStream();
    }

    @Override
    public void close() {
        if (process != null) process.destroyForcibly();
    }

    @Override
    public String getFullName() {
        return String.format("VLC (%s)", vlcPath);
    }

    @Override
    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(vlcPath, "--version");
        try (BufferedReader reader = proc.errorReader()) {
            String line = reader.readLine();
            return line == null;
        }
    }

    private void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    public static ProviderFactory<VLCTransportStreamProvider> factory(String vlcPath) {
        return () -> new VLCTransportStreamProvider(vlcPath);
    }

}

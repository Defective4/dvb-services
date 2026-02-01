package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
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
        process = ProcessUtils.start(vlcPath, "--sout", "#file{dst=/dev/stdout,mux=ts}", "--no-sout-all", "--sout-keep",
                adapter.input());
        return process.getInputStream();
    }

    @Override
    public void close() throws IOException {
        if (process != null) process.destroyForcibly();
    }

    @Override
    public void dumpPSI(AdapterInfo adapter, File target, long timeout) throws IOException {}

    @Override
    public String getFullName() {
        return null;
    }

    @Override
    public boolean isAvailable() throws IOException {
        return false;
    }

    private void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    public static TransportStreamProviderFactory<VLCTransportStreamProvider> factory(String vlcPath) {
        return () -> new VLCTransportStreamProvider(vlcPath);
    }

}

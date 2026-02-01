package io.github.defective4.tv.dvbservices.ts.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;

public class TSDuckProvider extends TransportStreamProvider {

    private static final String VERSION_STRING = "tsp: TSDuck - The MPEG Transport Stream Toolkit";
    private Process process;
    private final String tspExecutable;

    private InputStream tsStream;

    private TSDuckProvider(String tspExecutable) {
        this.tspExecutable = tspExecutable;
    }

    @Override
    public InputStream captureTS(AdapterInfo adapter, String service, boolean audioOnly) throws IOException {
        checkUsed();
        List<String> args = constructInitialParams(adapter);
        args.addAll(List.of("-P", "filter", "-p", "0", "-p", "17", "-p", "18", "--service", service));
        process = ProcessUtils.start(args.toArray(new String[0]));
        return process.getInputStream();
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    @Override
    public void dumpPSI(AdapterInfo adapter, File output, long timeout) throws IOException {
        checkUsed();
        List<String> arguments = constructInitialParams(adapter);
        arguments
                .addAll(List.of("-P", "filter", "--psi-si", "-P", "tables", "-p", "0", "-O", "file", output.getPath()));
        process = ProcessUtils.start(arguments.toArray(new String[0]));
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
    }

    @Override
    public String getFullName() {
        return "tsduck (" + tspExecutable + ")";
    }

    @Override
    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(tspExecutable, "--version");
        try (BufferedReader reader = new BufferedReader(proc.errorReader())) {
            String line = reader.readLine();
            return line != null && line.startsWith(VERSION_STRING);
        }
    }

    private void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    private List<String> constructInitialParams(AdapterInfo adapter) {
        List<String> arguments = new ArrayList<>();
        arguments.add(tspExecutable);
        arguments.add("-I");
        arguments.add(adapter.driver());
        for (Entry<String, String> entry : adapter.options().entrySet()) {
            arguments.add("--" + entry.getKey());
            arguments.add(entry.getValue());
        }
        Collections.addAll(arguments, adapter.args());
        return arguments;
    }

    public static TransportStreamProviderFactory<TSDuckProvider> factory(String tspExecutable) {
        return new TransportStreamProviderFactory<>() {

            @Override
            public TSDuckProvider create() {
                return new TSDuckProvider(tspExecutable);
            }
        };
    }

}

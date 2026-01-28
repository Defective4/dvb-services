package io.github.defective4.tv.dvbservices.ts.external;

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

public class TSDuckProvider extends TransportStreamProvider {

    private Process process;
    private final String tspExecutable;

    private InputStream tsStream;

    private TSDuckProvider(String tspExecutable) {
        this.tspExecutable = tspExecutable;
    }

    @Override
    public InputStream captureTS(AdapterInfo adapter) throws IOException {
        checkUsed();
        List<String> args = constructInitialParams(adapter);
        args.addAll(List.of());
        process = new ProcessBuilder(args.toArray(new String[0])).start();
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
        process = new ProcessBuilder(arguments.toArray(new String[0])).start();
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
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

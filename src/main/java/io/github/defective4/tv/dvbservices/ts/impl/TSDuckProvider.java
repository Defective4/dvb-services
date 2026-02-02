package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.Paths;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.ts.ProviderFactory;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;
import io.github.defective4.tv.dvbservices.util.TemporaryFiles;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class TSDuckProvider implements TransportStreamProvider, MetadataProvider {

    private static final String VERSION_STRING = "tsp: TSDuck - The MPEG Transport Stream Toolkit";
    private Process process;
    private final String tspExecutable;

    private InputStream tsStream;

    protected TSDuckProvider(String tspExecutable) {
        this.tspExecutable = tspExecutable;
    }

    @Override
    public MetadataResult captureMetadata(AdapterInfo adapter, long timeout)
            throws IOException, NotAnMPEGFileException, ParseException {
        checkUsed();
        File output = TemporaryFiles.getTemporaryFile("ts");
        List<String> arguments = constructInitialParams(adapter, true);
        arguments
                .addAll(List.of("-P", "filter", "--psi-si", "-P", "tables", "-p", "0", "-O", "file", output.getPath()));
        process = ProcessUtils.start(arguments.toArray(new String[0]));
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        close();
        return ElectronicProgramGuide.readEPG(output);
    }

    @Override
    public InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException {
        checkUsed();
        List<String> args = constructInitialParams(adapter, false);
        args.addAll(List.of("-P", "filter", "-p", "0", "-p", "17", "-p", "18", "--service", Integer.toString(service)));
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
    public String getFullName() {
        return "tsduck (" + tspExecutable + ")";
    }

    @Override
    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(tspExecutable, "--version");
        try (BufferedReader reader = new BufferedReader(proc.errorReader())) {
            String line = reader.readLine();
            proc.destroyForcibly();
            return line != null && line.startsWith(VERSION_STRING);
        }
    }

    protected void checkUsed() {
        if (process != null) throw new IllegalStateException("This stream provider was already used");
    }

    private List<String> constructInitialParams(AdapterInfo adapter, boolean meta) {
        List<String> arguments = new ArrayList<>();

        AdapterOptions ops = meta ? adapter.metadataCaptureOptions() : adapter.streamOptions();

        arguments.add(tspExecutable);
        arguments.add("-I");
        arguments.add(ops.driver());
        for (Entry<String, String> entry : ops.options().entrySet()) {
            arguments.add("--" + entry.getKey());
            arguments.add(entry.getValue());
        }
        Collections.addAll(arguments, ops.args());
        return arguments;
    }

    public static ProviderFactory<TSDuckProvider> factory(Paths paths) {
        return () -> new TSDuckProvider(paths.tspPath);
    }

    public static BiFunction<Integer, String, AdapterOptions> infoGenerator() {
        return (f, d) -> new AdapterOptions("dvb", Map.of("frequency", Integer.toString(f), "delivery-system", d),
                new String[0]);
    }

}

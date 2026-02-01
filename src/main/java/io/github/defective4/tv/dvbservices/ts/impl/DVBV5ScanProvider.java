package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import io.github.defective4.tv.dvbservices.toml.TOMLReader;
import io.github.defective4.tv.dvbservices.toml.TOMLWriter;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.ts.ProviderFactory;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class DVBV5ScanProvider implements MetadataProvider {

    private final String dvbv5ScanPath;
    private Process process;

    private DVBV5ScanProvider(String dvbv5ScanPath) {
        this.dvbv5ScanPath = dvbv5ScanPath;
    }

    @Override
    public MetadataResult captureMetadata(AdapterInfo adapter, long timeout)
            throws IOException, NotAnMPEGFileException, ParseException {
        List<String> args = new ArrayList<>(List.of(dvbv5ScanPath));
        args.addAll(List.of("-F", "-o", "/dev/stdout", "/dev/stdin"));
        AdapterOptions ops = adapter.metadataCaptureOptions();
        for (Entry<String, String> entry : ops.options().entrySet()) {
            args.add("--" + entry.getKey());
            args.add(entry.getValue());
        }
        Collections.addAll(args, ops.args());

        process = ProcessUtils.start(args.toArray(new String[0]));

        try (TOMLWriter writer = new TOMLWriter(process.outputWriter())) {
            String f = Integer.toString(adapter.freq());
            writer.write(Map.of(f, Map.of("FREQUENCY", f, "DELIVERY_SYSTEM",
                    adapter.metadataCaptureOptions().driver().toUpperCase())));
        }

        try (TOMLReader reader = new TOMLReader(process.inputReader(StandardCharsets.UTF_8))) {
            Map<String, Map<String, String>> toml = reader.read();
            process.destroy();
            Map<Integer, String> services = new LinkedHashMap<>();
            for (Entry<String, Map<String, String>> entry : toml.entrySet()) {
                String service = entry.getKey();
                int id;
                try {
                    id = Integer.parseInt(entry.getValue().get("SERVICE_ID"));
                } catch (Exception e) {
                    continue;
                }
                services.put(id, service);
            }

            Map<Integer, List<FriendlyEvent>> events = new HashMap<>();
            for (int i : services.keySet()) events.put(i, Collections.emptyList());

            return new MetadataResult(events, services);
        }
    }

    @Override
    public void close() {
        if (process != null) process.destroyForcibly();
    }

    @Override
    public String getFullName() {
        return "DVBv5 (" + dvbv5ScanPath + ")";
    }

    @Override
    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(dvbv5ScanPath, "-V");
        proc.destroyForcibly();
        return true;
    }

    public static ProviderFactory<DVBV5ScanProvider> factory(String dvbv5ScanPath) {
        return () -> new DVBV5ScanProvider(dvbv5ScanPath);
    }

}

package io.github.defective4.tv.dvbservices.http.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.ServiceNotFoundException;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.javalin.http.Context;

public class VideoController {
    private TransportStreamProvider provider;
    private final DVBServer server;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public VideoController(DVBServer server) {
        this.server = server;
    }

    public void serveVideo(Context ctx) throws ServiceNotFoundException, AdapterUnavailableException, IOException {
        String service = ctx.pathParam("service");
        if (service.toLowerCase().endsWith(".tg")) service = service.substring(0, service.length() - 3);
        String fsvc = service;

        Optional<AdapterInfo> adapterOpt = server.getMetadataController().getServiceAdapter(service);
        AdapterInfo adapter = adapterOpt
                .orElseThrow(() -> new ServiceNotFoundException("Service " + fsvc + " is not available"));

        if (provider != null) {
            throw new AdapterUnavailableException("Adapter for this service is not available");
        }

        try (TransportStreamProvider provider = server.getTspProviderFactory().create();
                InputStream in = provider.captureTS(adapter);
                OutputStream out = ctx.outputStream()) {
            this.provider = provider;

            while (true) {
                byte[] data = new byte[1024];
                int read = in.read(data);
                if (read < 0) break;
                // TODO filter
            }
        } finally {
            provider = null;
        }
    }
}

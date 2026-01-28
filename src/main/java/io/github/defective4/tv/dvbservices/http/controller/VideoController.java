package io.github.defective4.tv.dvbservices.http.controller;

import java.util.Optional;
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.ServiceNotFoundException;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.javalin.http.Context;

public class VideoController {
    private int lastFrequency;
    private TransportStreamProvider provider;
    private final DVBServer server;

    public VideoController(DVBServer server) {
        this.server = server;
    }

    public void serveVideo(Context ctx) throws ServiceNotFoundException, AdapterUnavailableException {
        String service = ctx.pathParam("service");
        if (service.toLowerCase().endsWith(".ts")) service = service.substring(0, service.length() - 3);
        String fsvc = service;

        Optional<AdapterInfo> adapterOpt = server.getMetadataController().getServiceAdapter(service);
        AdapterInfo adapter = adapterOpt
                .orElseThrow(() -> new ServiceNotFoundException("Service " + fsvc + " is not available"));

        if (provider != null) {
            if (lastFrequency != adapter.freq()) {
                throw new AdapterUnavailableException("Adapter for this service is not available");
            }
        } else {
            provider = server.getTspProviderFactory().create();
        }

        lastFrequency = adapter.freq();
    }
}

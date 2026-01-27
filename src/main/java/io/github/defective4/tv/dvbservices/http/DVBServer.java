package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;
import java.util.List;
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.controller.MetadataController;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.ts.external.TSDuckProvider;
import io.javalin.Javalin;

public class DVBServer {
    private final MetadataController epgController;
    private final Javalin javalin;
    private final TransportStreamProviderFactory<?> tspProvider;

    public DVBServer(List<AdapterInfo> adapters) {
        tspProvider = TSDuckProvider.factory("tsp");
        epgController = new MetadataController(adapters, "http://127.0.0.1", this);
        javalin = Javalin.create(cfg -> {
            cfg.router.apiBuilder(() -> {
                get("/tv.m3u", epgController::serveM3U);
                get("/epg.xml", epgController::serveXMLTV);
                get("/tv.xspf", epgController::serveXSPF);
            });
        });
    }

    public TransportStreamProviderFactory<?> getTspProvider() {
        return tspProvider;
    }

    public void start(String host, int port) {
        javalin.start(host, port);
    }
}

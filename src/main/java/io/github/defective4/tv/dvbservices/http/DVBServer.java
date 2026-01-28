package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import java.util.List;

import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.controller.ExceptionController;
import io.github.defective4.tv.dvbservices.http.controller.MetadataController;
import io.github.defective4.tv.dvbservices.http.controller.VideoController;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.ServiceNotFoundException;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.ts.external.TSDuckProvider;
import io.javalin.Javalin;

public class DVBServer {
    private final ExceptionController exceptionController = new ExceptionController();
    private final Javalin javalin;
    private final MetadataController metadataController;
    private final TransportStreamProviderFactory<?> tspProviderFactory;
    private final VideoController videoController;

    public DVBServer(List<AdapterInfo> adapters) {
        tspProviderFactory = TSDuckProvider.factory("tsp");
        metadataController = new MetadataController(adapters, "http://127.0.0.1", this);
        videoController = new VideoController(this);
        javalin = Javalin.create(cfg -> {
            cfg.router.apiBuilder(() -> {
                path("/meta", () -> {
                    get("/tv.m3u", metadataController::serveM3U);
                    get("/epg.xml", metadataController::serveXMLTV);
                    get("/tv.xspf", metadataController::serveXSPF);
                });

                path("/ts", () -> {
                    //
                    get("/{service}", videoController::serveVideo);
                });
            });
        });

        javalin.exception(ServiceNotFoundException.class, exceptionController::handleServiceNotFoundException);
        javalin.exception(AdapterUnavailableException.class, exceptionController::handleAdapterUnavailableException);
    }

    public MetadataController getMetadataController() {
        return metadataController;
    }

    public TransportStreamProviderFactory<?> getTspProviderFactory() {
        return tspProviderFactory;
    }

    public VideoController getVideoController() {
        return videoController;
    }

    public void start(String host, int port) {
        javalin.start(host, port);
    }
}

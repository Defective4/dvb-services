package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import io.github.defective4.tv.dvbservices.http.controller.APIController;
import io.github.defective4.tv.dvbservices.http.controller.ExceptionController;
import io.github.defective4.tv.dvbservices.http.controller.MetadataController;
import io.github.defective4.tv.dvbservices.http.controller.VideoController;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.settings.ServerSettings;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Metadata;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.ts.external.TSDuckProvider;
import io.javalin.Javalin;

public class DVBServer {
    private final APIController apiController;
    private final ExceptionController exceptionController = new ExceptionController();
    private final Javalin javalin;
    private final MetadataController metadataController;
    private final ServerSettings settings;
    private final TransportStreamProviderFactory<?> tspProviderFactory;
    private final VideoController videoController;

    public DVBServer(ServerSettings settings) {
        this.settings = settings;
        tspProviderFactory = TSDuckProvider.factory(settings.tools.tspPath);
        metadataController = new MetadataController(settings.getAdapters(), settings.server.baseURL, this);
        videoController = new VideoController(this);
        apiController = new APIController(this);
        javalin = Javalin.create(cfg -> {
            cfg.router.apiBuilder(() -> {
                path("/meta", () -> {
                    Metadata mtd = settings.metadata;
                    if (mtd.serveM3UPlaylist) get("/tv.m3u", metadataController::serveM3U);
                    if (mtd.serveXMLTV) get("/epg.xml", metadataController::serveXMLTV);
                    if (mtd.serveXSPFPlaylist) get("/tv.xspf", metadataController::serveXSPF);
                });

                path("/ts", () -> {
                    get("/{frequency}/{service}", videoController::serveVideoExact);
                    get("/{service}", videoController::serveVideo);
                });

            });
        });

        javalin.exception(IllegalArgumentException.class, exceptionController::handleArgumentException);
        javalin.exception(NotFoundException.class, exceptionController::handleNotFoundException);
        javalin.exception(AdapterUnavailableException.class, exceptionController::handleAdapterUnavailableException);
    }

    public MetadataController getMetadataController() {
        return metadataController;
    }

    public ServerSettings getSettings() {
        return settings;
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

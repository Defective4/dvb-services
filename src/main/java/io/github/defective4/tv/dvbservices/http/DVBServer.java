package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

import io.github.defective4.tv.dvbservices.http.controller.APIController;
import io.github.defective4.tv.dvbservices.http.controller.ExceptionController;
import io.github.defective4.tv.dvbservices.http.controller.MetadataController;
import io.github.defective4.tv.dvbservices.http.controller.StreamController;
import io.github.defective4.tv.dvbservices.http.exception.APIReadOnlyException;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.http.exception.UnauthorizedException;
import io.github.defective4.tv.dvbservices.settings.ServerSettings;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Metadata.Playlist;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.ts.external.TSDuckProvider;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;

public class DVBServer {
    private final APIController apiController;
    private final ExceptionController exceptionController = new ExceptionController();
    private final Javalin javalin;
    private final MetadataController metadataController;
    private final ServerSettings settings;
    private final TransportStreamProviderFactory<?> tspProviderFactory;
    private final StreamController videoController;

    public DVBServer(ServerSettings settings) {
        this.settings = settings;
        tspProviderFactory = TSDuckProvider.factory(settings.tools.tspPath);
        videoController = new StreamController(this);
        metadataController = new MetadataController(settings.getAdapters(), settings.server.baseURL, this);
        apiController = new APIController(this);
        javalin = Javalin.create(cfg -> {
            cfg.jsonMapper(new JavalinGson());
            cfg.router.apiBuilder(() -> {
                for (Playlist ps : settings.metadata.playlists) {
                    String name = ps.name;
                    String title = ps.title;
                    get("/playlist/" + name, ctx -> {
                        switch (ps.type) {
                            case M3U -> metadataController.serveM3U(ctx, title);
                            case XSPF -> metadataController.serveXSPF(ctx, title);
                            default -> { throw new IllegalArgumentException("Unknown playlist type " + ps.type); }
                        }
                    });
                }
                path("/meta",
                        () -> { if (settings.metadata.serveXMLTV) get("/epg.xml", metadataController::serveXMLTV); });

                path("/stream", () -> {
                    get("/{frequency}/{service}", videoController::serveVideoExact);
                    get("/{service}", videoController::serveVideo);
                });

                if (settings.api.enable) {
                    path("/api", () -> {
                        get("/status", apiController::getStatus);
                        get("/services", apiController::getServices);
                    });
                }
            });
        });

        javalin.exception(IllegalArgumentException.class, exceptionController::handleArgumentException);
        javalin.exception(NotFoundException.class, exceptionController::handleNotFoundException);
        javalin.exception(AdapterUnavailableException.class, exceptionController::handleAdapterUnavailableException);
        javalin.exception(UnauthorizedException.class, exceptionController::handleUnauthorizedException);
        javalin.exception(APIReadOnlyException.class, exceptionController::handleAPIReadOnlyException);
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

    public StreamController getVideoController() {
        return videoController;
    }

    public void start(String host, int port) {
        javalin.start(host, port);
    }
}

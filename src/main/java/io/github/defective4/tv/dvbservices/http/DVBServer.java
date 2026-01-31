package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

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
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProviderFactory;
import io.github.defective4.tv.dvbservices.ts.external.TSDuckProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.util.FFMpeg;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinGson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

public class DVBServer {
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final APIController apiController;
    private final ExceptionController exceptionController = new ExceptionController(this);
    private final Javalin javalin;
    private final Logger logger = new SimpleLoggerFactory().getLogger("dvb-server");
    private final MetadataController metadataController;
    private final ServerSettings settings;
    private final StreamController streamController;

    private final TransportStreamProviderFactory<?> tspProviderFactory;

    public DVBServer(ServerSettings settings) throws IOException {
        this.settings = settings;
        tspProviderFactory = TSDuckProvider.factory(settings.tools.tspPath);
        String providerName = null;
        try (TransportStreamProvider provider = tspProviderFactory.create()) {
            providerName = provider.getFullName();
            logger.info("Checking transport stream provider availability");
            if (!provider.isAvailable()) {
                throw new IOException();
            }
        } catch (IOException e) {
            logger.error(String.format(
                    "Transport Stream provider %s is not available. The server can't process streams without it.",
                    providerName));
            System.exit(5);
            throw e;
        }
        logger.info("Provider " + providerName + " OK");
        if (settings.server.needsTranscoding()) {
            try (FFMpeg ffmpeg = new FFMpeg(settings.tools.ffmpegPath)) {
                logger.info("Some media formats need transcording. Checking for ffmpeg");
                if (!ffmpeg.isAvailable()) {
                    throw new IOException();
                }
            } catch (IOException ex) {
                settings.server.formats = settings.server.formats.stream().filter(e -> e == MediaFormat.TS).toList();
                logger.warn("ffmpeg (" + settings.tools.ffmpegPath
                        + ") is not available. All media formats other than TS are disabled");
            }
            logger.info("ffmpeg OK");
        } else {
            logger.info("No transcoding need, ffmpeg check skipped");
        }
        streamController = new StreamController(this);
        metadataController = new MetadataController(settings.getAdapters(), settings.server.baseURL, this);
        apiController = new APIController(this);
        javalin = Javalin.create(cfg -> {
            cfg.registerPlugin(new OpenApiPlugin(ocfg -> {
                ocfg.withDefinitionConfiguration((t, dcfg) -> {
                    dcfg.withInfo(icfg -> {
                        icfg.contact("Defective4", "https://github.com/Defective4/dvb-services",
                                "def3ctive4@gmail.com");
                        icfg.license("MIT");
                        icfg.version("1.0");
                        icfg.title("DVB Services API");
                    });
                    dcfg.withSecurity(scfg -> scfg.withBearerAuth("token"));
                    dcfg.withServer(scfg -> scfg.url(settings.server.baseURL));
                });
            }));
            cfg.registerPlugin(new SwaggerPlugin());
            cfg.jsonMapper(new JavalinGson());
            cfg.router.apiBuilder(() -> {
                for (Playlist ps : settings.metadata.playlists) {
                    if (ps.type == null)
                        throw new IllegalArgumentException("One of the playlists has an unknown type.");
                    if (ps.format == null)
                        throw new IllegalArgumentException("One of the playlists has an unknown media format.");
                    String name = ps.name;
                    String title = ps.title;
                    MediaFormat fmt = ps.format;

                    get("/playlist/" + name, ctx -> {
                        switch (ps.type) {
                            case M3U -> metadataController.serveM3U(ctx, title, fmt);
                            case XSPF -> metadataController.serveXSPF(ctx, title, fmt);
                            case TEXT -> metadataController.serveTextPlaylist(ctx, fmt);
                            default -> { throw new IllegalArgumentException("Unknown playlist type " + ps.type); }
                        }
                    });
                }
                path("/meta",
                        () -> { if (settings.metadata.serveXMLTV) get("/epg.xml", metadataController::serveXMLTV); });

                path("/stream", () -> {
                    get("/{frequency}/{service}", streamController::serveVideoExact);
                    get("/{service}", streamController::serveVideo);
                });

                if (settings.api.enable) {
                    path("/api", () -> {
                        get("/status", apiController::getStatus);
                        path("/metadata", () -> {
                            get("/services", apiController::getServices);
                            post("/scanner", apiController::handleScanner);
                            get("/epg", apiController::getEPG);
                        });
                    });
                }
            });
        });

        javalin.exception(IllegalStateException.class, exceptionController::handleStateException);
        javalin.exception(IllegalArgumentException.class, exceptionController::handleArgumentException);
        javalin.exception(NotFoundException.class, exceptionController::handleNotFoundException);
        javalin.exception(AdapterUnavailableException.class, exceptionController::handleAdapterUnavailableException);
        javalin.exception(UnauthorizedException.class, exceptionController::handleUnauthorizedException);
        javalin.exception(APIReadOnlyException.class, exceptionController::handleAPIReadOnlyException);
    }

    public Logger getLogger() {
        return logger;
    }

    public MetadataController getMetadataController() {
        return metadataController;
    }

    public ServerSettings getSettings() {
        return settings;
    }

    public StreamController getStreamController() {
        return streamController;
    }

    public TransportStreamProviderFactory<?> getTspProviderFactory() {
        return tspProviderFactory;
    }

    public void logClientActivity(Context ctx, String msg) {
        logger.info(String.format("[%s] [%s] %s", ctx.ip(), ctx.statusCode(), msg));
    }

    public void logClientError(Context ctx, String msg) {
        logger.warn(String.format("[%s] [%s] %s", ctx.ip(), ctx.statusCode(), msg));
    }

    public void start(String host, int port) {
        javalin.start(host, port);
        metadataController.schedule();
    }
}

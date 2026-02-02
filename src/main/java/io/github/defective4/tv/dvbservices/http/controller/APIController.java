package io.github.defective4.tv.dvbservices.http.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.APIReadOnlyException;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.UnauthorizedException;
import io.github.defective4.tv.dvbservices.http.model.APIServices;
import io.github.defective4.tv.dvbservices.http.model.APIStatus;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.http.model.AdapterState;
import io.github.defective4.tv.dvbservices.http.model.EPG;
import io.github.defective4.tv.dvbservices.http.model.ScannerAction;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.settings.ServerSettings;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.Paths;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.ProviderType;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

public class APIController {

    public final DVBServer server;

    private final ExecutorService scannerService = Executors.newSingleThreadExecutor();

    private Future<Object> scannerTask;

    public APIController(DVBServer dvbServer) {
        server = dvbServer;
    }

    @OpenApi(tags = "API", path = "/api/config/generate", methods = HttpMethod.GET, queryParams = {
            @OpenApiParam(allowEmptyValue = false, description = "Frequencies to generate adapters for", name = "frequencies", example = "538000000,562000000", required = true),
            @OpenApiParam(allowEmptyValue = false, description = "TV delivery system", name = "system", example = "dvb-t2", required = false),
            @OpenApiParam(allowEmptyValue = false, description = "Stream provider type", name = "streamProvider", required = true, type = ProviderType.class),
            @OpenApiParam(allowEmptyValue = false, description = "Metadata provider type", name = "metadataProvider", required = true, type = ProviderType.class) }, summary = "Generates adapter info config to be inserted in the configuration file")
    public void generateConfig(Context ctx) {
        int[] frequencies = ctx.queryParamAsClass("frequencies", int[].class)
                .getOrThrow((c) -> new IllegalArgumentException("Frequencies list is missing"));
        String system = ctx.queryParamAsClass("system", String.class).getOrDefault("dvb-t2");
        ProviderType streamProvider = ctx.queryParamAsClass("streamProvider", ProviderType.class)
                .getOrThrow(arg0 -> new IllegalArgumentException("Stream provider is missing or invalid"));
        ProviderType metadataProvider = ctx.queryParamAsClass("metadataProvider", ProviderType.class)
                .getOrThrow(arg0 -> new IllegalArgumentException("Metadata provider is missing or invalid"));

        streamProvider.getAs(TransportStreamProvider.class, new Paths());
        metadataProvider.getAs(MetadataProvider.class, new Paths());

        ServerSettings settings = new ServerSettings();
        try {
            for (Field field : settings.getClass().getFields()) {
                if (field.canAccess(settings) && !Modifier.isFinal(field.getModifiers())) field.set(settings, null);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        BiFunction<Integer, String, AdapterOptions> streamInfoGenerator = streamProvider.getInfoGenerator();
        BiFunction<Integer, String, AdapterOptions> metaInfoGenerator = metadataProvider.getInfoGenerator();

        settings.adapters = new ArrayList<>();

        for (int freq : frequencies) {
            settings.adapters.add(new AdapterInfo(metaInfoGenerator.apply(freq, system),
                    streamInfoGenerator.apply(freq, system), freq));
        }

        ctx.json(settings);
    }

    @OpenApi(tags = "API", path = "/api/metadata/epg", security = @OpenApiSecurity(name = "token"), methods = HttpMethod.GET, summary = "Get current EPG", responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = EPG.class, mimeType = ContentType.JSON)))
    public void getEPG(Context ctx) throws UnauthorizedException {
        authorizeR(ctx);
        ctx.json(new EPG(server.getMetadataController().getEpg()));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "API", path = "/api/metadata/services", security = @OpenApiSecurity(name = "token"), methods = HttpMethod.GET, summary = "Get a list of services", responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = APIServices.class, mimeType = ContentType.JSON)))
    public void getServices(Context ctx) throws UnauthorizedException {
        authorizeR(ctx);
        Map<Integer, Collection<TVService>> table = server.getMetadataController().getServiceMap();

        ctx.json(new APIServices(table));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "API", path = "/api/metadata/epg", security = @OpenApiSecurity(name = "token"), methods = HttpMethod.GET, summary = "Get adapter status", responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = APIStatus.class, mimeType = ContentType.JSON)))
    public void getStatus(Context ctx) throws UnauthorizedException {
        authorizeR(ctx);
        ctx.json(new APIStatus(server.getStreamController().isWatching() ? AdapterState.WATCHING
                : server.getMetadataController().isDumping() ? AdapterState.CAPTURING_EPG : AdapterState.AVAILABLE));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "API", path = "/api/metadata/scanner", security = @OpenApiSecurity(name = "token"), methods = HttpMethod.POST, summary = "Control metadata scanner", responses = @OpenApiResponse(status = "200"), formParams = @OpenApiParam(allowEmptyValue = false, required = true, type = ScannerAction.class, name = "action", example = "SCAN"))
    public void handleScanner(Context ctx)
            throws UnauthorizedException, APIReadOnlyException, AdapterUnavailableException {
        String act = ctx.formParam("action");
        if (act == null) throw new IllegalArgumentException("Missing action param");
        ScannerAction action;
        try {
            action = ScannerAction.valueOf(act.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid action param");
        }
        switch (action) {
            case SCAN -> scanMetadata(ctx);
            case ENABLE -> toggleMetadataScanner(ctx, true);
            case DISABLE -> toggleMetadataScanner(ctx, false);
            default -> {}
        }
        server.logClientActivity(ctx, ctx.path());
    }

    private void authorize(Context ctx) throws UnauthorizedException {
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
            if (token.equals(server.getSettings().api.token)) return;
        }
        throw new UnauthorizedException("You are not authorized to use this endpoint.");
    }

    private void authorizeR(Context ctx) throws UnauthorizedException {
        if (server.getSettings().api.protectReadEndpoints) authorize(ctx);
    }

    private void authorizeW(Context ctx) throws UnauthorizedException, APIReadOnlyException {
        if (server.getSettings().api.readOnly)
            throw new APIReadOnlyException("This server's API is set to read-only mode");
        if (server.getSettings().api.protectWriteEndpoints) authorize(ctx);
    }

    private void scanMetadata(Context ctx)
            throws UnauthorizedException, APIReadOnlyException, AdapterUnavailableException {
        authorizeW(ctx);
        MetadataController metadataController = server.getMetadataController();
        if (scannerTask != null && !scannerTask.isDone() && !scannerTask.isCancelled()) {
            throw new IllegalStateException("A metadata scanner task is already pending");
        }
        if (server.getStreamController().isWatching() || metadataController.isDumping())
            throw new AdapterUnavailableException("The adapter is in use");

        scannerTask = scannerService.submit(() -> {
            metadataController.captureEPG(true);
            return null;
        });

        ctx.result("Scan started");
        server.logClientActivity(ctx, "Manual metadata scan started");
    }

    private void toggleMetadataScanner(Context ctx, boolean enable) {
        server.getSettings().metadata.scheduleMetaCapture = enable;
        ctx.result(String.format("Metadata scanner %s", enable ? "enabled" : "disabled"));
    }
}

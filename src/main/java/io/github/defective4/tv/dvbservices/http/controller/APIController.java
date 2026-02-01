package io.github.defective4.tv.dvbservices.http.controller;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.APIReadOnlyException;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.UnauthorizedException;
import io.github.defective4.tv.dvbservices.http.model.APIServices;
import io.github.defective4.tv.dvbservices.http.model.APIStatus;
import io.github.defective4.tv.dvbservices.http.model.AdapterState;
import io.github.defective4.tv.dvbservices.http.model.EPG;
import io.github.defective4.tv.dvbservices.http.model.ScannerAction;
import io.github.defective4.tv.dvbservices.http.model.TVService;
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

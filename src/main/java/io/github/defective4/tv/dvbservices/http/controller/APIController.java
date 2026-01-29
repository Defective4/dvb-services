package io.github.defective4.tv.dvbservices.http.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.APIReadOnlyException;
import io.github.defective4.tv.dvbservices.http.exception.UnauthorizedException;
import io.github.defective4.tv.dvbservices.http.model.APIServices;
import io.github.defective4.tv.dvbservices.http.model.APIStatus;
import io.github.defective4.tv.dvbservices.http.model.AdapterState;
import io.javalin.http.Context;

public class APIController {

    private final DVBServer server;

    public APIController(DVBServer dvbServer) {
        server = dvbServer;
    }

    public void getServices(Context ctx) throws UnauthorizedException {
        authorizeR(ctx);
        Map<String, AdapterInfo> table = server.getMetadataController().getAdapterTable();
        Map<String, Integer> services = new LinkedHashMap<>();
        table.entrySet().forEach(t -> services.put(t.getKey(), t.getValue().freq()));

        ctx.json(new APIServices(services));
    }

    public void getStatus(Context ctx) throws UnauthorizedException {
        authorizeR(ctx);
        ctx.json(new APIStatus(server.getVideoController().isWatching() ? AdapterState.WATCHING
                : server.getMetadataController().isDumping() ? AdapterState.CAPTURING_EPG : AdapterState.AVAILABLE));
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
}

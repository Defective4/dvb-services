package io.github.defective4.tv.dvbservices.http.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.model.APIServices;
import io.github.defective4.tv.dvbservices.http.model.APIStatus;
import io.github.defective4.tv.dvbservices.http.model.AdapterState;
import io.javalin.http.Context;

public class APIController {

    private final DVBServer server;

    public APIController(DVBServer dvbServer) {
        server = dvbServer;
    }

    public void getServices(Context ctx) {
        Map<String, AdapterInfo> table = server.getMetadataController().getAdapterTable();
        Map<String, Integer> services = new LinkedHashMap<>();
        table.entrySet().forEach(t -> services.put(t.getKey(), t.getValue().freq()));

        ctx.json(new APIServices(services));
    }

    public void getStatus(Context ctx) {
        ctx.json(new APIStatus(server.getVideoController().isWatching() ? AdapterState.WATCHING
                : server.getMetadataController().isDumping() ? AdapterState.CAPTURING_EPG : AdapterState.AVAILABLE));
    }
}

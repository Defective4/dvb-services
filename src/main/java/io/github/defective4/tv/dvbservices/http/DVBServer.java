package io.github.defective4.tv.dvbservices.http;

import static io.javalin.apibuilder.ApiBuilder.get;

import io.github.defective4.tv.dvbservices.http.controller.MetadataController;
import io.javalin.Javalin;

public class DVBServer {
    private final MetadataController epgController;
    private final Javalin javalin;

    public DVBServer() {
        epgController = new MetadataController(new float[] { 538e6f }, "http://127.0.0.1");
        javalin = Javalin.create(cfg -> {
            cfg.router.apiBuilder(() -> {
                get("/tv.m3u", epgController::serveM3U);
                get("/epg.xml", epgController::serveXMLTV);
                get("/tv.xspf", epgController::serveXSPF);
            });
        });
    }

    public void start(String host, int port) {
        javalin.start(host, port);
    }
}

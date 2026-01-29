package io.github.defective4.tv.dvbservices.http.controller;

import io.github.defective4.tv.dvbservices.http.DVBServer;

public class APIController {

    private final DVBServer server;

    public APIController(DVBServer dvbServer) {
        server = dvbServer;
    }

}

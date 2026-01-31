package io.github.defective4.tv.dvbservices.http.controller;

import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.APIReadOnlyException;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.http.exception.UnauthorizedException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ExceptionController {

    private final DVBServer server;

    public ExceptionController(DVBServer server) {
        this.server = server;
    }

    public void handleAdapterUnavailableException(AdapterUnavailableException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public void handleAPIReadOnlyException(APIReadOnlyException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.FORBIDDEN);
    }

    public void handleArgumentException(IllegalArgumentException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.BAD_REQUEST);
    }

    public void handleNotFoundException(NotFoundException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.NOT_FOUND);
    }

    public void handleStateException(IllegalStateException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.FORBIDDEN);
    }

    public void handleUnauthorizedException(UnauthorizedException ex, Context ctx) {
        simpleResponse(ctx, ex, HttpStatus.UNAUTHORIZED);
    }

    private void simpleResponse(Context ctx, Exception ex, HttpStatus status) {
        ctx.result(ex.getMessage());
        ctx.status(status);
        server.logClientError(ctx, ctx.path());
    }
}

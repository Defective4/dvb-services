package io.github.defective4.tv.dvbservices.http.controller;

import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ExceptionController {
    public void handleAdapterUnavailableException(AdapterUnavailableException ex, Context ctx) {
        ctx.result(ex.getMessage());
        ctx.status(HttpStatus.SERVICE_UNAVAILABLE);
    }

    public void handleArgumentException(IllegalArgumentException ex, Context ctx) {
        ctx.result(ex.getMessage());
        ctx.status(HttpStatus.BAD_REQUEST);
    }

    public void handleNotFoundException(NotFoundException ex, Context ctx) {
        ctx.result(ex.getMessage());
        ctx.status(HttpStatus.NOT_FOUND);
    }
}

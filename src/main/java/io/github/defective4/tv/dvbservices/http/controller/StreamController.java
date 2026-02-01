package io.github.defective4.tv.dvbservices.http.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaConverter;
import io.github.defective4.tv.dvbservices.ts.Provider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;

public class StreamController {
    private Provider provider;
    private final DVBServer server;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public StreamController(DVBServer server) {
        this.server = server;
    }

    public boolean isWatching() {
        return provider != null;
    }

    @OpenApi(methods = HttpMethod.GET, path = "/stream/{service}", pathParams = @OpenApiParam(allowEmptyValue = false, example = "TVP1.ts", description = "Service name along with media extension", required = true, name = "service"), summary = "Stream a TV service", tags = "Stream", responses = {
            @OpenApiResponse(description = "Media stream with undefined length", status = "200"),
            @OpenApiResponse(status = "404"), @OpenApiResponse(status = "500") })
    public void serveVideo(Context ctx) throws NotFoundException, AdapterUnavailableException, IOException {
        checkAvailable();
        Entry<TVService, String> serviceEntry = getService(ctx);
        TVService service = serviceEntry.getKey();
        AdapterInfo adapter;
        MetadataController ctl = server.getMetadataController();
        adapter = ctl.getServiceAdapter(service)
                .orElseThrow(() -> new NotFoundException("Service " + service + " is not available"));
        String type = serviceEntry.getValue();

        serveService(ctx, service, adapter, type);
    }

    @OpenApi(methods = HttpMethod.GET, path = "/stream/{frequency}/{service}", pathParams = {
            @OpenApiParam(allowEmptyValue = false, example = "TVP1.ts", description = "Service name along with media extension", required = true, name = "service"),
            @OpenApiParam(allowEmptyValue = false, example = "538000000", description = "Frequency (Hz) of the service", required = true, name = "frequency", type = int.class) }, summary = "Stream a TV service", tags = "Stream", responses = {
                    @OpenApiResponse(description = "Media stream with undefined length", status = "200"),
                    @OpenApiResponse(status = "404"), @OpenApiResponse(status = "400"),
                    @OpenApiResponse(status = "500") })
    public void serveVideoExact(Context ctx) throws NotFoundException, AdapterUnavailableException, IOException {
        checkAvailable();
        MetadataController ctl = server.getMetadataController();

        Entry<TVService, String> entry = getService(ctx);
        TVService service = entry.getKey();
        String type = entry.getValue();
        int frequency = ctx.pathParamAsClass("frequency", int.class)
                .getOrThrow(arg0 -> new IllegalArgumentException("The frequency must be a valid number"));

        AdapterInfo adapter = ctl.getServiceAdapter(frequency)
                .orElseThrow(() -> new NotFoundException("There is no adapter for this frequency"));

        serveService(ctx, service, adapter, type);
    }

    private void checkAvailable() throws AdapterUnavailableException {
        if (isWatching()) {
            throw new AdapterUnavailableException("Adapter for this service is not available");
        }

        MetadataController mtd = server.getMetadataController();
        if (mtd.isDumping()) {
            throw new AdapterUnavailableException(
                    String.format("The server is currently capturing TV metadata. Current progress: %s/%s",
                            mtd.getDumpingProgress(), mtd.getAdapters().size()));
        }
    }

    private Map.Entry<TVService, String> getService(Context ctx) throws NotFoundException {
        String serviceName = ctx.pathParam("service");
        int dotIndex = serviceName.indexOf('.');
        if (dotIndex < 0) throw new IllegalArgumentException("Missing file name extension");
        String type = serviceName.substring(dotIndex + 1).toLowerCase();
        serviceName = serviceName.substring(0, dotIndex);
        String fsvc = serviceName;

        Optional<TVService> opt;
        try {
            int id;
            if (fsvc.startsWith("0x"))
                id = Integer.parseInt(fsvc.substring(2));
            else
                id = Integer.parseInt(fsvc, 16);
            opt = server.getMetadataController().getService(id);
        } catch (NumberFormatException e) {
            opt = server.getMetadataController().getService(serviceName);
        }
        TVService service = opt.orElseThrow(() -> new NotFoundException("Service " + fsvc + " is not available"));
        return Map.entry(service, type);
    }

    private void serveService(Context ctx, TVService service, AdapterInfo adapter, String type)
            throws NotFoundException, IOException {

        MediaFormat fmt;
        try {
            fmt = MediaFormat.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown media format: " + type);
        }

        if (!server.getSettings().server.formats.contains(fmt)) {
            throw new NotFoundException(String.format("This server does not serve %s files", fmt.name()));
        }

        try (TransportStreamProvider provider = server.getStreamProviderFactory().create();
                InputStream in = provider.captureTS(adapter, service.id(), !fmt.isVideo());
                OutputStream out = ctx.outputStream()) {
            this.provider = provider;

            server.logClientActivity(ctx, String.format("Started streaming service %s with format %s", service, fmt));

            if (fmt.isVideo()) {
                byte[] data = new byte[1024];
                int read;
                while (true) {
                    read = in.read(data);
                    if (read < 0) break;
                    out.write(data, 0, read);
                }
            } else {
                try (MediaConverter converter = server.getMediaConverterFactory().create()) {
                    if (!converter.isFormatSupported(fmt)) {
                        throw new UnsupportedOperationException(
                                converter.getName() + " does not support this media format.");
                    }
                    String opts = server.getSettings().server.audio.converterParams;
                    converter.convert(in, out, fmt, opts.isBlank() ? new String[0] : opts.split(" "));
                    converter.closePeacefully();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            provider = null;
            server.logClientActivity(ctx, String.format("Stopped streaming service %s with format %s", service, fmt));
        }
    }
}

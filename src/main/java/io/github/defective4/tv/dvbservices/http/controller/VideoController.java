package io.github.defective4.tv.dvbservices.http.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.AdapterUnavailableException;
import io.github.defective4.tv.dvbservices.http.exception.ServiceNotFoundException;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.util.FFMpeg;
import io.javalin.http.Context;

public class VideoController {
    private TransportStreamProvider provider;
    private final DVBServer server;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public VideoController(DVBServer server) {
        this.server = server;
    }

    public void serveVideo(Context ctx) throws ServiceNotFoundException, AdapterUnavailableException, IOException {
        checkDumping();
        Entry<String, String> serviceEntry = getService(ctx);
        String service = serviceEntry.getKey();
        AdapterInfo adapter = server.getMetadataController().getServiceAdapter(service)
                .orElseThrow(() -> new ServiceNotFoundException("Service " + service + " is not available"));
        String type = serviceEntry.getValue();

        serveService(ctx, service, adapter, type);
    }

    public void serveVideoExact(Context ctx) throws ServiceNotFoundException, AdapterUnavailableException, IOException {
        checkDumping();
        Entry<String, String> entry = getService(ctx);
        String service = entry.getKey();
        String type = entry.getValue();
        int frequency = ctx.pathParamAsClass("frequency", int.class)
                .getOrThrow(arg0 -> new IllegalArgumentException("The frequency must be a valid number"));
        AdapterInfo adapter = server.getMetadataController().getServiceAdapter(frequency)
                .orElseThrow(() -> new AdapterUnavailableException("There is no adapter for this frequency"));

        serveService(ctx, service, adapter, type);
    }

    private void checkDumping() throws AdapterUnavailableException {
        MetadataController mtd = server.getMetadataController();
        if (mtd.isDumping()) {
            throw new AdapterUnavailableException(
                    String.format("The server is currently capturing TV metadata. Current progress: %s/%s",
                            mtd.getDumpingProgress(), mtd.getAdapters().size()));
        }
    }

    private void serveService(Context ctx, String service, AdapterInfo adapter, String type)
            throws ServiceNotFoundException, AdapterUnavailableException, IOException {
        boolean video;

        switch (type) {
            case "ts": {
                video = true;
                break;
            }
            case "mp3": {
                video = false;
                break;
            }
            default:
                throw new ServiceNotFoundException("Unknown file extension ." + type);
        }

        if (provider != null) {
            throw new AdapterUnavailableException("Adapter for this service is not available");
        }

        try (TransportStreamProvider provider = server.getTspProviderFactory().create();
                InputStream in = provider.captureTS(adapter, service, !video);
                OutputStream out = ctx.outputStream()) {
            this.provider = provider;

            if (!video) {
                try (FFMpeg ffmpeg = new FFMpeg("ffmpeg")) {
                    ffmpeg.convertToMP3(in, out);
                    ffmpeg.closePeacefully();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                byte[] data = new byte[1024];
                int read;
                while (true) {
                    read = in.read(data);
                    if (read < 0) break;
                    out.write(data, 0, read);
                }
            }
        } finally {
            provider = null;
        }
    }

    private static Map.Entry<String, String> getService(Context ctx) throws ServiceNotFoundException {
        String service = ctx.pathParam("service");
        int dotIndex = service.indexOf('.');
        if (dotIndex < 0) throw new ServiceNotFoundException("Missing file name extension");
        String type = service.substring(dotIndex + 1).toLowerCase();
        service = service.substring(0, dotIndex);
        return Map.entry(service, type);
    }
}

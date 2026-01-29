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
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.util.FFMpeg;
import io.javalin.http.Context;

public class StreamController {
    private TransportStreamProvider provider;
    private final DVBServer server;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public StreamController(DVBServer server) {
        this.server = server;
    }

    public boolean isWatching() {
        return provider != null;
    }

    public void serveVideo(Context ctx) throws NotFoundException, AdapterUnavailableException, IOException {
        checkAvailable();
        Entry<String, String> serviceEntry = getService(ctx);
        String service = serviceEntry.getKey();
        AdapterInfo adapter = server.getMetadataController().getServiceAdapter(service)
                .orElseThrow(() -> new NotFoundException("Service " + service + " is not available"));
        String type = serviceEntry.getValue();

        serveService(ctx, service, adapter, type);
    }

    public void serveVideoExact(Context ctx) throws NotFoundException, AdapterUnavailableException, IOException {
        checkAvailable();
        Entry<String, String> entry = getService(ctx);
        String service = entry.getKey();
        String type = entry.getValue();
        int frequency = ctx.pathParamAsClass("frequency", int.class)
                .getOrThrow(arg0 -> new IllegalArgumentException("The frequency must be a valid number"));
        AdapterInfo adapter = server.getMetadataController().getServiceAdapter(frequency)
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

    private void serveService(Context ctx, String service, AdapterInfo adapter, String type)
            throws NotFoundException, IOException {

        MediaFormat fmt;

        switch (type) {
            case "ts": {
                if (!server.getSettings().server.video.serveTS) {
                    throw new NotFoundException("This server does not serve video files");
                }
                fmt = MediaFormat.TS;
                break;
            }
            case "mp3": {
                if (!server.getSettings().server.audio.serveMP3) {
                    throw new NotFoundException("This server does not serve mp3 files");
                }
                fmt = MediaFormat.MP3;
                break;
            }
            case "wav": {
                if (!server.getSettings().server.audio.serveWAV) {
                    throw new NotFoundException("This server does not serve wav files");
                }
                fmt = MediaFormat.WAV;
                break;
            }
            default:
                throw new NotFoundException("Unknown file extension ." + type);
        }

        try (TransportStreamProvider provider = server.getTspProviderFactory().create();
                InputStream in = provider.captureTS(adapter, service, !fmt.isVideo());
                OutputStream out = ctx.outputStream()) {
            this.provider = provider;

            if (fmt.isVideo()) {
                byte[] data = new byte[1024];
                int read;
                while (true) {
                    read = in.read(data);
                    if (read < 0) break;
                    out.write(data, 0, read);
                }
            } else {
                try (FFMpeg ffmpeg = new FFMpeg(server.getSettings().tools.ffmpegPath)) {
                    ffmpeg.convertToMP3(in, out, fmt, server.getSettings().server.audio.ffmpegOpts.split(" "));
                    ffmpeg.closePeacefully();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            provider = null;
        }
    }

    private static Map.Entry<String, String> getService(Context ctx) {
        String service = ctx.pathParam("service");
        int dotIndex = service.indexOf('.');
        if (dotIndex < 0) throw new IllegalArgumentException("Missing file name extension");
        String type = service.substring(dotIndex + 1).toLowerCase();
        service = service.substring(0, dotIndex);
        return Map.entry(service, type);
    }
}

package io.github.defective4.tv.dvbservices.http.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import com.google.gson.Gson;

import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaFormat;
import io.github.defective4.tv.dvbservices.playlist.M3UPlaylist;
import io.github.defective4.tv.dvbservices.playlist.PlaintextPlaylist;
import io.github.defective4.tv.dvbservices.playlist.XSPFPlaylist;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Cache;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.util.HashUtil;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class MetadataController {

    private static final String M3U_MIME = "audio/x-mpegurl";
    private static final String XSPF_MIME = "application/xspf+xml";

    private final List<AdapterInfo> adapters;

    private final Map<Integer, AdapterInfo> adapterTable = new HashMap<>();
    private final String baseURL;
    private int dumpingProgress;
    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private boolean isDumping;
    private final DVBServer server;
    private final Map<Integer, Collection<TVService>> serviceMap = new LinkedHashMap<>();
    private final Timer timer = new Timer(true);

    public MetadataController(List<AdapterInfo> adapters, String baseURL, DVBServer server) {
        this.baseURL = baseURL;
        this.adapters = Collections.unmodifiableList(adapters);
        this.server = server;
    }

    public boolean captureEPG() {
        return captureEPG(false);
    }

    public boolean captureEPG(boolean ignoreCache) {
        if (server.getStreamController().isWatching() || isDumping()) return false;
        isDumping = true;
        dumpingProgress = 0;
        try {
            adapterTable.clear();
            serviceMap.clear();
            epg.clear();

            Map<AdapterInfo, File> files = new LinkedHashMap<>();
            List<AdapterInfo> cached = new ArrayList<>();

            if (server.getSettings().cache.enableMetadataCache) {
                Cache cache = server.getSettings().cache;
                int size = 0;
                for (AdapterInfo adapter : adapters) {
                    try {
                        File dir = cache.getCacheDirectory();
                        dir.mkdirs();

                        String hash = adapter.calculateString();
                        File dataFile = new File(dir, hash + ".bin");
                        File metaFile = new File(dataFile.getPath() + ".meta");

                        long timestamp = 0;
                        byte[] binHash = new byte[16];
                        if (metaFile.isFile() && !ignoreCache) {
                            try (DataInputStream in = new DataInputStream(new FileInputStream(metaFile))) {
                                timestamp = in.readLong();
                                in.readFully(binHash);
                            }
                        }

                        long diff = (System.currentTimeMillis() - timestamp) / 1000;

                        if (dataFile.isFile() && !ignoreCache && diff < cache.cacheTTL
                                && HashUtil.hashEquals(dataFile, binHash)) {
                            try (Reader reader = new FileReader(dataFile, StandardCharsets.UTF_8)) {
                                MetadataResult result = gson.fromJson(reader, MetadataResult.class);
                                putResult(adapter, result);
                                cached.add(adapter);
                                size++;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (size > 0) server.getLogger().info("Found " + size + " cached service(s)");
            }

            for (AdapterInfo adapter : adapters) {
                if (cached.contains(adapter)) continue;
                try (MetadataProvider ts = server.getMetaProviderFactory().create()) {
                    MetadataResult result = ts.captureMetadata(adapter,
                            TimeUnit.SECONDS.toMillis(server.getSettings().metadata.metaCaptureTimeout));
                    cache(adapter, ignoreCache, result);
                    putResult(adapter, result);
                    dumpingProgress++;
                } catch (IOException | NotAnMPEGFileException | ParseException e) {
                    e.printStackTrace();
                }
            }

            for (Entry<AdapterInfo, File> entry : files.entrySet()) {
                entry.getValue().delete();
            }

        } finally {
            isDumping = false;
        }
        return true;
    }

    public List<AdapterInfo> getAdapters() {
        return adapters;
    }

    public Map<Integer, AdapterInfo> getAdapterTable() {
        return Collections.unmodifiableMap(adapterTable);
    }

    public int getDumpingProgress() {
        return dumpingProgress;
    }

    public Map<String, List<FriendlyEvent>> getEpg() {
        return Collections.unmodifiableMap(epg);
    }

    public Optional<TVService> getService(int id) {
        return serviceMap.values().stream().map(col -> col.stream().filter(tv -> tv.id() == id).findAny())
                .filter(Optional::isPresent).map(Optional::get).findAny();
    }

    public Optional<TVService> getService(String name) {
        return serviceMap.values().stream().map(col -> col.stream().filter(tv -> tv.name().equals(name)).findAny())
                .filter(Optional::isPresent).map(Optional::get).findAny();
    }

    public Optional<AdapterInfo> getServiceAdapter(int frequency) {
        return adapters.stream().filter(adapter -> adapter.frequency() == frequency).findAny();
    }

    public Optional<AdapterInfo> getServiceAdapter(TVService service) {
        return Optional.ofNullable(adapterTable.get(service.id()));
    }

    public Map<Integer, Collection<TVService>> getServiceMap() {
        return Collections.unmodifiableMap(serviceMap);
    }

    public boolean isDumping() {
        return isDumping;
    }

    public void schedule() {
        timer.scheduleAtFixedRate(new TimerTask() {

            private int time;
            private final int timeout = server.getSettings().metadata.metaCaptureIntervalMinutes;

            {
                time = timeout;
            }

            @Override
            public void run() {
                time++;
                if (time >= timeout) {
                    if (!server.getSettings().metadata.scheduleMetaCapture) {
                        time = timeout - 1;
                        return;
                    }
                    long dur = server.getSettings().getAdapters().size()
                            * server.getSettings().metadata.metaCaptureTimeout * 1000;
                    server.getLogger().info("Starting scheduled metadata capture. Estimated end: "
                            + DVBServer.DATE_FORMAT.format(new Date(System.currentTimeMillis() + dur)));
                    if (!captureEPG())
                        time = timeout - 1;
                    else {
                        time = 0;
                        server.getLogger().info("Metadata capture finished. Next capture: " + DVBServer.DATE_FORMAT
                                .format(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeout))));
                    }
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
    }

    @OpenApi(tags = "Metadata", path = "/playlist/{playlist}.m3u", methods = HttpMethod.GET, pathParams = @OpenApiParam(allowEmptyValue = false, description = "Playlist name", example = "tv", name = "playlist", required = true), responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = M3U_MIME)) })
    public void serveM3U(Context ctx, String title, MediaFormat format) {
        ctx.contentType(M3U_MIME);
        ctx.result(new M3UPlaylist(serviceMap, baseURL).save(title, format));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/playlist/{playlist}.txt", methods = HttpMethod.GET, pathParams = @OpenApiParam(allowEmptyValue = false, description = "Playlist name", example = "tv", name = "playlist", required = true), responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.PLAIN)) })
    public void serveTextPlaylist(Context ctx, MediaFormat format) throws IOException {
        ctx.contentType(ContentType.TEXT_PLAIN);
        ctx.result(new PlaintextPlaylist(serviceMap, baseURL).save(null, format));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/meta/epg.xml", methods = HttpMethod.GET, responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.XML)) })
    public void serveXMLTV(Context ctx) throws TransformerException {
        String xmltv = ElectronicProgramGuide.generateXmlTV(epg);
        ctx.contentType(ContentType.XML);
        ctx.result(xmltv);
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/playlist/{playlist}.xspf", methods = HttpMethod.GET, pathParams = @OpenApiParam(allowEmptyValue = false, description = "Playlist name", example = "tv", name = "playlist", required = true), responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.PLAIN)) })
    public void serveXSPF(Context ctx, String title, MediaFormat format) throws IOException {
        ctx.contentType(XSPF_MIME);
        ctx.result(new XSPFPlaylist(serviceMap, baseURL).save(title, format));
        server.logClientActivity(ctx, ctx.path());
    }

    private void cache(AdapterInfo adapter, boolean ignoreCache, MetadataResult result) throws IOException {
        Cache cache = server.getSettings().cache;
        File dataFile = null;
        File metaFile = null;

        if (cache.enableMetadataCache) {
            File dir = cache.getCacheDirectory();
            dir.mkdirs();
            String hash = adapter.calculateString();
            dataFile = new File(dir, hash + ".bin");
            metaFile = new File(dataFile.getPath() + ".meta");
        }

        if (dataFile != null && metaFile != null) {
            try (Writer writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                gson.toJson(result, writer);
            }

            try (DataOutputStream output = new DataOutputStream(new FileOutputStream(metaFile))) {
                output.writeLong(System.currentTimeMillis());
                output.write(HashUtil.hash(dataFile));
            }
        }
    }

    private void putResult(AdapterInfo adapter, MetadataResult result) {
        for (int svc : result.services().keySet()) adapterTable.put(svc, adapter);
        Map<String, List<FriendlyEvent>> events = new LinkedHashMap<>();
        for (Entry<Integer, List<FriendlyEvent>> entry : result.events().entrySet()) {
            String svc = result.services().get(entry.getKey());
            if (svc == null) continue;
            events.put(svc, entry.getValue());
        }
        serviceMap.computeIfAbsent(adapter.freq(), t -> new ArrayList<>()).addAll(
                result.services().entrySet().stream().map(e -> new TVService(e.getValue(), e.getKey())).toList());
        epg.putAll(events);
    }
}

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
import io.github.defective4.tv.dvbservices.AdapterInfo;
import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.http.exception.NotFoundException;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Cache;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.M3UPlaylist;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.ts.playlist.PlaintextPlaylist;
import io.github.defective4.tv.dvbservices.ts.playlist.XSPFPlaylist;
import io.github.defective4.tv.dvbservices.util.HashUtil;
import io.github.defective4.tv.dvbservices.util.TemporaryFiles;
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

    private final Map<String, AdapterInfo> adapterTable = new HashMap<>();
    private final String baseURL;
    private int dumpingProgress;
    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private boolean isDumping;
    private final DVBServer server;
    private final Map<Integer, Collection<String>> serviceMap = new LinkedHashMap<>();
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
        if (server.getStreamController().isWatching() || isDumping()
                || !server.getSettings().metadata.enableMetaCapture)
            return false;
        isDumping = true;
        dumpingProgress = 0;
        try {
            adapterTable.clear();
            serviceMap.clear();
            epg.clear();

            Map<AdapterInfo, File> files = new LinkedHashMap<>();
            for (AdapterInfo adapter : adapters) {
                try (TransportStreamProvider ts = server.getTspProviderFactory().create()) {
                    File file = TemporaryFiles.getTemporaryFile(".ts");
                    files.put(adapter, file);

                    ts.dumpPSI(adapter, file,
                            TimeUnit.SECONDS.toMillis(server.getSettings().metadata.metaCaptureTimeout));
                    dumpingProgress++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (Entry<AdapterInfo, File> entry : files.entrySet()) {
                try {
                    Map<String, List<FriendlyEvent>> result = getOrCached(entry.getKey(), entry.getValue(),
                            ignoreCache);

                    for (String svc : result.keySet()) adapterTable.put(svc, entry.getKey());
                    entry.getValue().delete();
                    serviceMap.computeIfAbsent(entry.getKey().freq(), t -> new ArrayList<>()).addAll(result.keySet());
                    epg.putAll(result);
                } catch (NotAnMPEGFileException | IOException | ParseException e) {
                    e.printStackTrace();
                }
            }

        } finally {
            isDumping = false;
        }
        return true;
    }

    public List<AdapterInfo> getAdapters() {
        return adapters;
    }

    public Map<String, AdapterInfo> getAdapterTable() {
        return Collections.unmodifiableMap(adapterTable);
    }

    public int getDumpingProgress() {
        return dumpingProgress;
    }

    public Map<String, List<FriendlyEvent>> getEpg() {
        return Collections.unmodifiableMap(epg);
    }

    public Optional<AdapterInfo> getServiceAdapter(int frequency) {
        return adapters.stream().filter(adapter -> adapter.freq() == frequency).findAny();
    }

    public Optional<AdapterInfo> getServiceAdapter(String service) {
        return Optional.ofNullable(adapterTable.get(service));
    }

    public boolean isDumping() {
        return isDumping;
    }

    public void schedule() {
        if (!server.getSettings().metadata.enableMetaCapture) return;
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
    public void serveM3U(Context ctx, String title, MediaFormat format) throws NotFoundException {
        server.getSettings().metadata.checkMetaCapture();
        ctx.contentType(M3U_MIME);
        ctx.result(new M3UPlaylist(serviceMap, baseURL).save(title, format));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/playlist/{playlist}.txt", methods = HttpMethod.GET, pathParams = @OpenApiParam(allowEmptyValue = false, description = "Playlist name", example = "tv", name = "playlist", required = true), responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.PLAIN)) })
    public void serveTextPlaylist(Context ctx, MediaFormat format) throws IOException, NotFoundException {
        server.getSettings().metadata.checkMetaCapture();
        ctx.contentType(ContentType.TEXT_PLAIN);
        ctx.result(new PlaintextPlaylist(serviceMap, baseURL).save(null, format));
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/meta/epg.xml", methods = HttpMethod.GET, responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.XML)) })
    public void serveXMLTV(Context ctx) throws TransformerException, NotFoundException {
        server.getSettings().metadata.checkMetaCapture();
        String xmltv = ElectronicProgramGuide.generateXmlTV(epg);
        ctx.contentType(ContentType.XML);
        ctx.result(xmltv);
        server.logClientActivity(ctx, ctx.path());
    }

    @OpenApi(tags = "Metadata", path = "/playlist/{playlist}.xspf", methods = HttpMethod.GET, pathParams = @OpenApiParam(allowEmptyValue = false, description = "Playlist name", example = "tv", name = "playlist", required = true), responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(mimeType = ContentType.PLAIN)) })
    public void serveXSPF(Context ctx, String title, MediaFormat format) throws IOException, NotFoundException {
        server.getSettings().metadata.checkMetaCapture();
        ctx.contentType(XSPF_MIME);
        ctx.result(new XSPFPlaylist(serviceMap, baseURL).save(title, format));
        server.logClientActivity(ctx, ctx.path());
    }

    private Map<String, List<FriendlyEvent>> getOrCached(AdapterInfo adapter, File file, boolean ignoreCache)
            throws NotAnMPEGFileException, IOException, ParseException {
        Map<String, List<FriendlyEvent>> result = null;
        Cache cache = server.getSettings().cache;
        File dataFile = null;
        File metaFile = null;

        if (cache.enableMetadataCache) {
            File dir = cache.getCacheDirectory();
            dir.mkdirs();

            String hash = adapter.calculateString();
            dataFile = new File(dir, hash + ".bin");
            metaFile = new File(dataFile.getPath() + ".meta");

            long timestamp = 0;
            byte[] binHash = new byte[16];
            if (metaFile.isFile() && !ignoreCache) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(metaFile))) {
                    timestamp = in.readLong();
                    in.readFully(binHash);
                }
            }

            long diff = (System.currentTimeMillis() - timestamp) / 1000;

            if (dataFile.isFile() && !ignoreCache && diff < cache.cacheTTL && HashUtil.hashEquals(dataFile, binHash)) {
                try (Reader reader = new FileReader(dataFile, StandardCharsets.UTF_8)) {
                    result = gson.fromJson(reader, Map.class);
                }
            }
        }

        if (result == null) {
            result = ElectronicProgramGuide.readEPG(file);
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
        return result;
    }
}

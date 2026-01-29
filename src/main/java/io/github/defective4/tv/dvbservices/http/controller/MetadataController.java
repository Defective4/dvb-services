package io.github.defective4.tv.dvbservices.http.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Cache;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.M3UPlaylist;
import io.github.defective4.tv.dvbservices.ts.playlist.Playlist;
import io.github.defective4.tv.dvbservices.ts.playlist.XSPFPlaylist;
import io.github.defective4.tv.dvbservices.util.HashUtil;
import io.github.defective4.tv.dvbservices.util.TemporaryFiles;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class MetadataController {

    private static final String M3U_MIME = "audio/x-mpegurl";
    private static final String XSPF_MIME = "application/xspf+xml";
    private static final String XSPF_PLAYLIST_TITLE = "TV Playlist";

    private final List<AdapterInfo> adapters;

    private final Map<String, AdapterInfo> adapterTable = new HashMap<>();
    private final String baseURL;
    private int dumpingProgress;
    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private boolean isDumping;
    private Playlist m3uPlaylist, xspfPlaylist;
    private final DVBServer server;
    private final Timer timer = new Timer(true);

    public MetadataController(List<AdapterInfo> adapters, String baseURL, DVBServer server) {
        this.baseURL = baseURL;
        m3uPlaylist = new M3UPlaylist(Map.of(), baseURL);
        xspfPlaylist = new XSPFPlaylist(Map.of(), baseURL, XSPF_PLAYLIST_TITLE);
        this.adapters = Collections.unmodifiableList(adapters);
        this.server = server;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                captureEPG();
            }
        }, 0, TimeUnit.MINUTES.toMillis(server.getSettings().metadata.epgRefreshIntervalMinutes));
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

    public Optional<AdapterInfo> getServiceAdapter(int frequency) {
        return adapters.stream().filter(adapter -> adapter.freq() == frequency).findAny();
    }

    public Optional<AdapterInfo> getServiceAdapter(String service) {
        return Optional.ofNullable(adapterTable.get(service));
    }

    public boolean isDumping() {
        return isDumping;
    }

    public void serveM3U(Context ctx) throws IOException {
        ctx.contentType(M3U_MIME);
        try (Writer writer = new OutputStreamWriter(ctx.outputStream())) {
            m3uPlaylist.save(writer);
        }
    }

    public void serveXMLTV(Context ctx) throws TransformerException {
        String xmltv = ElectronicProgramGuide.generateXmlTV(epg);
        ctx.contentType(ContentType.XML);
        ctx.result(xmltv);
    }

    public void serveXSPF(Context ctx) throws IOException {
        ctx.contentType(XSPF_MIME);
        try (Writer writer = new OutputStreamWriter(ctx.outputStream())) {
            xspfPlaylist.save(writer);
        }
    }

    private void captureEPG() {
        if(server.getVideoController().isWatching()) return;
        if(isDumping()) return;
        isDumping = true;
        dumpingProgress = 0;
        try {
            adapterTable.clear();
            epg.clear();

            Map<AdapterInfo, File> files = new LinkedHashMap<>();
            for (AdapterInfo adapter : adapters) {
                try (TransportStreamProvider ts = server.getTspProviderFactory().create()) {
                    File file = TemporaryFiles.getTemporaryFile(".ts");
                    files.put(adapter, file);

                    ts.dumpPSI(adapter, file,
                            TimeUnit.SECONDS.toMillis(server.getSettings().metadata.epgCaptureTimeout));
                    dumpingProgress++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Map<Integer, Collection<String>> serviceMap = new LinkedHashMap<>();

            for (Entry<AdapterInfo, File> entry : files.entrySet()) {
                try {
                    Map<String, List<FriendlyEvent>> result = getOrCached(entry.getKey(), entry.getValue());

                    for (String svc : result.keySet()) adapterTable.put(svc, entry.getKey());
                    entry.getValue().delete();
                    serviceMap.computeIfAbsent(entry.getKey().freq(), t -> new ArrayList<>()).addAll(result.keySet());
                    epg.putAll(result);
                } catch (NotAnMPEGFileException | IOException | ParseException e) {
                    e.printStackTrace();
                }
            }

            m3uPlaylist = new M3UPlaylist(serviceMap, baseURL);
            xspfPlaylist = new XSPFPlaylist(serviceMap, baseURL, XSPF_PLAYLIST_TITLE);
        } finally {
            isDumping = false;
        }
    }

    private Map<String, List<FriendlyEvent>> getOrCached(AdapterInfo adapter, File file)
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
            if (metaFile.isFile()) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(metaFile))) {
                    timestamp = in.readLong();
                    in.readFully(binHash);
                }
            }

            long diff = (System.currentTimeMillis() - timestamp) / 1000;

            if (dataFile.isFile() && diff < cache.cacheTTL && HashUtil.hashEquals(dataFile, binHash)) {
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

package io.github.defective4.tv.dvbservices.http.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.github.defective4.tv.dvbservices.TemporaryFiles;
import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.ts.M3UPlaylist;
import io.github.defective4.tv.dvbservices.ts.Playlist;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.XSPFPlaylist;
import io.github.defective4.tv.dvbservices.util.DOMUtils;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class MetadataController {

    private static final String M3U_MIME = "audio/x-mpegurl";
    private static final String XSPF_MIME = "application/xspf+xml";
    private static final String XSPF_PLAYLIST_TITLE = "TV Playlist";

    private final String baseURL;
    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final float[] frequencies;
    private Playlist m3uPlaylist, xspfPlaylist;
    private final Map<Integer, Document> patTables = new HashMap<>();
    private final Map<Integer, Document> sdtTables = new HashMap<>();
    private final DVBServer server;
    private final Timer timer = new Timer(true);

    public MetadataController(float[] frequencies, String baseURL, DVBServer server) {
        this.frequencies = frequencies;
        this.baseURL = baseURL;
        m3uPlaylist = new M3UPlaylist(Map.of(), baseURL);
        xspfPlaylist = new XSPFPlaylist(Map.of(), baseURL, XSPF_PLAYLIST_TITLE);
        this.server = server;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    captureEPG();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.DAYS.toMillis(1));
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

    private void captureEPG() throws IOException {
        synchronized (epg) {
            epg.clear();
        }
        File dir = TemporaryFiles.getTemporaryDir();
        Map<Integer, File> files = new LinkedHashMap<>();
        for (float freq : frequencies) {
            try (TransportStreamProvider ts = server.getTspProvider().create()) {
                int f = (int) freq;
                File file = new File(dir, f + ".ts");
                files.put(f, file);

                File patFile = TemporaryFiles.getTemporaryFile("xml");
                File sdtFile = TemporaryFiles.getTemporaryFile("xml");

                ts.dumpPSI(f, file, patFile, sdtFile, TimeUnit.SECONDS.toMillis(30));

                Document pat = DOMUtils.DOC_BUILDER.parse(patFile);
                Document sdt = DOMUtils.DOC_BUILDER.parse(sdtFile);

                patFile.delete();
                sdtFile.delete();

                patTables.put(f, pat);
                sdtTables.put(f, sdt);
            } catch (IOException | SAXException e) {
                e.printStackTrace();
            }
        }

        Map<Integer, Collection<String>> serviceMap = new LinkedHashMap<>();

        synchronized (epg) {
            for (Entry<Integer, File> entry : files.entrySet()) {
                try {
                    Map<String, List<FriendlyEvent>> result;
                    result = ElectronicProgramGuide.readEPG(entry.getValue());
                    serviceMap.computeIfAbsent(entry.getKey(), t -> new ArrayList<>()).addAll(result.keySet());
                    epg.putAll(result);
                } catch (NotAnMPEGFileException | IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        m3uPlaylist = new M3UPlaylist(serviceMap, baseURL);
        xspfPlaylist = new XSPFPlaylist(serviceMap, baseURL, XSPF_PLAYLIST_TITLE);
    }
}

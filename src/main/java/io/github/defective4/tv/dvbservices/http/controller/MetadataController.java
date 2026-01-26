package io.github.defective4.tv.dvbservices.http.controller;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import io.github.defective4.tv.dvbservices.TemporaryDirs;
import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.ts.M3UPlaylist;
import io.github.defective4.tv.dvbservices.ts.external.TestTSProvider;
import io.github.defective4.tv.dvbservices.ts.external.TransportStreamProvider;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class MetadataController {

    private final String baseURL;
    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final float[] frequencies;
    private M3UPlaylist m3uPlaylist;
    private final Timer timer = new Timer(true);

    public MetadataController(float[] frequencies, String baseURL) {
        this.frequencies = frequencies;
        this.baseURL = baseURL;
        m3uPlaylist = new M3UPlaylist(Map.of(), baseURL);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    captureEPG();
                } catch (IOException | NotAnMPEGFileException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.DAYS.toMillis(1));
    }

    public void serveM3U(Context ctx) {
        StringWriter writer = new StringWriter();
        m3uPlaylist.save(writer);
        ctx.contentType("audio/x-mpegurl");
        ctx.result(writer.toString());
    }

    public void serveXMLTV(Context ctx) throws TransformerException {
        String xmltv = ElectronicProgramGuide.generateXmlTV(epg);
        ctx.contentType(ContentType.XML);
        ctx.result(xmltv);
    }

    private void captureEPG() throws IOException, NotAnMPEGFileException, ParseException {
        synchronized (epg) {
            epg.clear();
        }
        File dir = TemporaryDirs.getTemporaryDir();
        Map<Integer, File> files = new LinkedHashMap<>();
        for (float freq : frequencies) {
            try (TransportStreamProvider ts = new TestTSProvider()) {
                int f = (int) freq;
                File file = new File(dir, f + ".ts");
                files.put(f, file);
                ts.dumpPSI(f, file, TimeUnit.SECONDS.toMillis(30));
            }
        }

        Map<Integer, Collection<String>> serviceMap = new LinkedHashMap<>();

        synchronized (epg) {
            for (Entry<Integer, File> entry : files.entrySet()) {
                Map<String, List<FriendlyEvent>> result = ElectronicProgramGuide.readEPG(entry.getValue());
                serviceMap.computeIfAbsent(entry.getKey(), t -> new ArrayList<>()).addAll(result.keySet());
                epg.putAll(result);
            }
        }

        m3uPlaylist = new M3UPlaylist(serviceMap, baseURL);
    }
}

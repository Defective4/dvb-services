package io.github.defective4.tv.dvbservices.http.controller;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.TransformerException;

import io.github.defective4.tv.dvbservices.TemporaryDirs;
import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.ts.external.TSDuck;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class MetadataController {

    private final Map<String, List<FriendlyEvent>> epg = new LinkedHashMap<>();
    private final float[] frequencies;
    private final Timer timer = new Timer(true);

    public MetadataController(float[] frequencies) {
        this.frequencies = frequencies;
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
        List<File> files = new ArrayList<>();
        for (float freq : frequencies) {
            try (TSDuck ts = new TSDuck()) {
                int f = (int) freq;
                File file = new File(dir, f + ".ts");
                files.add(file);
                ts.dumpPSI(f, file, TimeUnit.SECONDS.toMillis(30));
            }
        }

        synchronized (epg) {
            for (File entry : files) {
                Map<String, List<FriendlyEvent>> result = ElectronicProgramGuide.readEPG(entry);
                epg.putAll(result);
            }
        }
    }
}

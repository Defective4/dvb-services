package io.github.defective4.tv.dvbservices;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.ts.M3UPlaylist;

public class Main {
    public static void main(String[] args) {
        try {
            Map<String, List<FriendlyEvent>> epg = ElectronicProgramGuide.readEPG(new File("/tmp/tv.ts"));
            String xml = ElectronicProgramGuide.generateXmlTV(epg);
            try (Writer wr = new FileWriter("/tmp/tv.xml", StandardCharsets.UTF_8)) {
                wr.write(xml);
            }

            int freq = (int) 538e6f;

            try (Writer wr = new FileWriter("/tmp/tv.m3u", StandardCharsets.UTF_8)) {
                M3UPlaylist playlist = new M3UPlaylist(Map.of(freq, epg.keySet()), "http://127.0.0.1:8081");
                playlist.save(wr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

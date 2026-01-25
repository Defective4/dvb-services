package io.github.defective4.tv.dvbservices;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;

public class Main {
    public static void main(String[] args) {
        try {
            Map<String, List<FriendlyEvent>> epg = ElectronicProgramGuide.readEPG(new File("/tmp/tv.ts"));
            String xml = ElectronicProgramGuide.generateXmlTV(epg);
            try (Writer wr = new FileWriter("/tmp/tv.xml", StandardCharsets.UTF_8)) {
                wr.write(xml);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

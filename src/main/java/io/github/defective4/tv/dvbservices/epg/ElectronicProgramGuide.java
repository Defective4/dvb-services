package io.github.defective4.tv.dvbservices.epg;

import static io.github.defective4.tv.dvbservices.util.DOMUtils.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import nl.digitalekabeltelevisie.data.mpeg.PSI;
import nl.digitalekabeltelevisie.data.mpeg.TransportStream;
import nl.digitalekabeltelevisie.data.mpeg.psi.EIT;
import nl.digitalekabeltelevisie.data.mpeg.psi.EITsection;
import nl.digitalekabeltelevisie.data.mpeg.psi.EITsection.Event;
import nl.digitalekabeltelevisie.data.mpeg.psi.SDT;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;
import nl.digitalekabeltelevisie.util.ServiceIdentification;

public class ElectronicProgramGuide {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss +0000");

    private ElectronicProgramGuide() {}

    public static String generateXmlTV(Map<String, List<FriendlyEvent>> epg) throws TransformerException {
        Document document = DOC_BUILDER.newDocument();
        Element tv = document.createElement("tv");
        tv.setAttribute("date", DATE_FORMAT.format(new Date(System.currentTimeMillis())));

        createChannelElements(document, epg.keySet()).forEach(chan -> tv.appendChild(chan));
        createProgrammeElements(document, epg).forEach(prog -> tv.appendChild(prog));

        document.appendChild(tv);
        StringWriter writer = new StringWriter();
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<!DOCTYPE tv SYSTEM \"xmltv.dtd\">\n\n");
        XMLTV_TRANSFORMER.transform(new DOMSource(document), new StreamResult(writer));
        writer.flush();
        return writer.toString();
    }

    public static MetadataResult readEPG(File tsFile) throws NotAnMPEGFileException, IOException, ParseException {
        TransportStream stream = new TransportStream(tsFile);
        stream.parseStream(null);
        PSI psi = stream.getPsi();
        SDT sdt = psi.getSdt();
        EIT eit = psi.getEit();

        Map<ServiceIdentification, EITsection[]> schedule = eit.getCombinedSchedule();
        Map<Integer, List<FriendlyEvent>> events = new LinkedHashMap<>();
        Map<Integer, String> pat = new HashMap<>();
        for (Entry<ServiceIdentification, EITsection[]> scheduleEntry : schedule.entrySet()) {
            ServiceIdentification service = scheduleEntry.getKey();
            String serviceName = sdt.getServiceName(service.originalNetworkId(), service.transportStreamId(),
                    service.serviceId());
            pat.put(service.serviceId(), serviceName);

            for (EITsection section : scheduleEntry.getValue()) {
                if (section == null) continue;
                for (Event event : section.getEventList()) {
                    FriendlyEvent fe = FriendlyEvent.fromEvent(event);
                    events.computeIfAbsent(service.serviceId(), t -> new ArrayList<>()).add(fe);
                }
            }
        }

        return new MetadataResult(Collections.unmodifiableMap(events), Collections.unmodifiableMap(pat));
    }

    private static List<Element> createChannelElements(Document doc, Set<String> services) {
        List<Element> channels = new ArrayList<>();
        for (String service : services) {
            Element channel = doc.createElement("channel");
            channel.setAttribute("id", service);
            Element dname = doc.createElement("display-name");
            dname.setTextContent(service);

            channel.appendChild(dname);
            channels.add(channel);
        }
        return Collections.unmodifiableList(channels);
    }

    private static List<Element> createProgrammeElements(Document doc, Map<String, List<FriendlyEvent>> epg) {
        List<Element> programmes = new ArrayList<>();
        for (Entry<String, List<FriendlyEvent>> entry : epg.entrySet()) {
            for (FriendlyEvent event : entry.getValue()) {
                Element programme = doc.createElement("programme");
                Date end = new Date(event.getStart().getTime() + TimeUnit.MINUTES.toMillis(event.getMinutes()));
                programme.setAttribute("start", DATE_FORMAT.format(event.getStart()));
                programme.setAttribute("stop", DATE_FORMAT.format(end));
                programme.setAttribute("channel", entry.getKey());

                Element title = doc.createElement("title");
                title.setTextContent(event.getName());
                Element desc = doc.createElement("desc");
                desc.setTextContent(event.getDescription());

                programme.appendChild(title);
                programme.appendChild(desc);

                for (String genre : event.getGenres()) {
                    Element cat = doc.createElement("category");
                    cat.setTextContent(genre);
                    programme.appendChild(cat);
                }

                for (String word : event.getKeywords()) {
                    Element kw = doc.createElement("keyword");
                    kw.setTextContent(word);
                    programme.appendChild(kw);
                }

                Element len = doc.createElement("length");
                len.setAttribute("units", "minutes");
                len.setTextContent(Long.toString(event.getMinutes()));
                programme.appendChild(len);

                Element present;

                present = doc.createElement("present");
                present.setTextContent("true");
                Element video = doc.createElement("video");
                video.appendChild(present);

                present = doc.createElement("present");
                present.setTextContent("true");
                Element audio = doc.createElement("audio");
                audio.appendChild(present);

                programme.appendChild(video);
                programme.appendChild(audio);

                programmes.add(programme);
            }
        }
        return Collections.unmodifiableList(programmes);
    }
}

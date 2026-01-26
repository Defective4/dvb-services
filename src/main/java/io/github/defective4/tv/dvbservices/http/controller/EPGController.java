package io.github.defective4.tv.dvbservices.http.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import io.github.defective4.tv.dvbservices.epg.ElectronicProgramGuide;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.javalin.http.ContentType;
import io.javalin.http.Context;

public class EPGController {

    private final Map<String, List<FriendlyEvent>> epg = Collections.emptyMap();

    public void serveXMLTV(Context ctx) throws TransformerException {
        String xmltv = ElectronicProgramGuide.generateXmlTV(epg);
        ctx.contentType(ContentType.XML);
        ctx.result(xmltv);
    }
}

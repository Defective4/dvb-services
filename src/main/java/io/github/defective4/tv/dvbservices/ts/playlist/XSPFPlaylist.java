package io.github.defective4.tv.dvbservices.ts.playlist;

import static io.github.defective4.tv.dvbservices.util.DOMUtils.createAndAppendElement;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import io.github.defective4.tv.dvbservices.util.DOMUtils;

public class XSPFPlaylist extends Playlist {

    private final String title;

    public XSPFPlaylist(Map<Integer, Collection<String>> services, String baseURL, String title) {
        super(services, baseURL);
        this.title = title;
    }

    @Override
    public void save(Writer writer) throws IOException {
        Document root = DOMUtils.DOC_BUILDER.newDocument();
        createAndAppendElement(root, "playlist", playlist -> {
            playlist.setAttribute("xmlns", "http://xspf.org/ns/0/");
            playlist.setAttribute("xmlns:vlc", "http://www.videolan.org/vlc/playlist/ns/0/");
            playlist.setAttribute("version", "1");
            createAndAppendElement(playlist, "title", title -> title.setTextContent(XSPFPlaylist.this.title));
            createAndAppendElement(playlist, "trackList", tracks -> {
                for (Entry<Integer, Collection<String>> entry : getServices().entrySet()) {
                    int freq = entry.getKey();
                    int id = 0;
                    for (String service : entry.getValue()) {
                        int fid = id;
                        createAndAppendElement(tracks, "track", track -> {
                            createAndAppendElement(track, "location",
                                    loc -> loc.setTextContent(String.format("%s/stream/%s/%s", getBaseURL(), freq,
                                            URLEncoder.encode(service, StandardCharsets.UTF_8) + ".ts")));
                            createAndAppendElement(track, "title", title -> title.setTextContent(service));
                            createAndAppendElement(track, "extension", ext -> {
                                ext.setAttribute("application", "http://www.videolan.org/vlc/playlist/0");
                                createAndAppendElement(ext, "vlc:id", vid -> vid.setTextContent(Integer.toString(fid)));
                            });
                        });
                        id++;
                    }
                }
            });
            createAndAppendElement(playlist, "extension", ext -> {
                ext.setAttribute("application", "http://www.videolan.org/vlc/playlist/0");
                createAndAppendElement(ext, "vlc:item", item -> item.setAttribute("tid", "0"));
            });
        });

        StringWriter result = new StringWriter();
        try {
            DOMUtils.XML_TRANSFORMER.transform(new DOMSource(root), new StreamResult(result));
        } catch (TransformerException ex) {
            throw new IllegalStateException(ex);
        }
        writer.write(result.toString());
        writer.flush();
    }

}

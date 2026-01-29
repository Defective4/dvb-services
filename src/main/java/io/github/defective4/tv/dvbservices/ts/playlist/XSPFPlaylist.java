package io.github.defective4.tv.dvbservices.ts.playlist;

import static io.github.defective4.tv.dvbservices.util.DOMUtils.createAndAppendElement;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import io.github.defective4.tv.dvbservices.util.DOMUtils;

public class XSPFPlaylist extends Playlist {

    public XSPFPlaylist(Map<Integer, Collection<String>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title) throws IOException {
        Document root = DOMUtils.DOC_BUILDER.newDocument();
        createAndAppendElement(root, "playlist", playlist -> {
            playlist.setAttribute("xmlns", "http://xspf.org/ns/0/");
            playlist.setAttribute("xmlns:vlc", "http://www.videolan.org/vlc/playlist/ns/0/");
            playlist.setAttribute("version", "1");
            createAndAppendElement(playlist, "title", e -> e.setTextContent(title));
            createAndAppendElement(playlist, "trackList", tracks -> {
                for (Entry<Integer, Collection<String>> entry : getServices().entrySet()) {
                    int freq = entry.getKey();
                    int id = 0;
                    for (String service : entry.getValue()) {
                        int fid = id;
                        createAndAppendElement(tracks, "track", track -> {
                            createAndAppendElement(track, "location", loc -> loc.setTextContent(format(freq, service)));
                            createAndAppendElement(track, "title", e -> e.setTextContent(service));
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
        return result.toString();
    }

}

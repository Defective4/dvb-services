package io.github.defective4.tv.dvbservices.playlist;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaFormat;

public class PlaintextPlaylist extends Playlist {

    public PlaintextPlaylist(Map<Integer, Collection<TVService>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title, MediaFormat format) throws IOException {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            for (Entry<Integer, Collection<TVService>> entry : getServices().entrySet()) {
                entry.getValue().forEach(svc -> pw.println(format(entry.getKey(), svc.name(), format)));
            }
        }
        return writer.toString();
    }

}

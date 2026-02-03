package io.github.defective4.tv.dvbservices.playlist;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaFormat;

public class PLSPlaylist extends Playlist {

    public PLSPlaylist(Map<Integer, Collection<TVService>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title, MediaFormat format) throws IOException {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println("[playlist]");

            int i = 0;
            for (Entry<Integer, Collection<TVService>> entry : getServices().entrySet()) {
                for (TVService service : entry.getValue()) {
                    i++;
                    pw.println();
                    pw.println(String.format("File%s=%s", i, format(entry.getKey(), service.name(), format)));
                    pw.println(String.format("Title%s=%s", i, service.name()));
                }
            }

            pw.println();
            pw.println("NumberOfEntries=" + i);
            pw.println("Version=2");
        }
        return writer.toString();
    }

}

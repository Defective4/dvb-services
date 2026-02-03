package io.github.defective4.tv.dvbservices.playlist;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaFormat;

public class M3UPlaylist extends Playlist {

    public M3UPlaylist(Map<Integer, Collection<TVService>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title, MediaFormat format) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println("#EXTM3U");
            pw.println("#PLAYLIST:" + title);
            for (Entry<Integer, Collection<TVService>> entry : getServices().entrySet()) {
                int freq = entry.getKey();
                for (TVService service : entry.getValue()) {
                    pw.println("#EXTINF:0," + service.name());
                    pw.println(format(freq, service.name(), format));
                }
            }
        }
        return writer.toString();
    }

}

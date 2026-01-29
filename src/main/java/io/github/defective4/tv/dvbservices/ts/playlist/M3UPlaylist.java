package io.github.defective4.tv.dvbservices.ts.playlist;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class M3UPlaylist extends Playlist {

    public M3UPlaylist(Map<Integer, Collection<String>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title, MediaFormat format) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println("#EXTM3U");
            pw.println("#PLAYLIST:" + title);
            for (Entry<Integer, Collection<String>> entry : getServices().entrySet()) {
                int freq = entry.getKey();
                for (String service : entry.getValue()) {
                    pw.println("#EXTINF:0," + service);
                    pw.println(format(freq, service, format));
                }
            }
        }
        return writer.toString();
    }

}

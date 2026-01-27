package io.github.defective4.tv.dvbservices.ts;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class M3UPlaylist extends Playlist {

    public M3UPlaylist(Map<Integer, Collection<String>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public void save(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);
        pw.println("#EXTM3U");
        for (Entry<Integer, Collection<String>> entry : getServices().entrySet()) {
            int freq = entry.getKey();
            for (String service : entry.getValue()) {
                pw.println("#EXTINF:0," + service);
                pw.println(String.format("%s/%s/%s", getBaseURL(), freq,
                        URLEncoder.encode(service, StandardCharsets.UTF_8) + ".ts"));
            }
        }
        pw.flush();
    }

}

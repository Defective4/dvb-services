package io.github.defective4.tv.dvbservices.ts;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class M3UPlaylist {
    private final String baseURL;
    private final Map<Integer, Collection<String>> services;

    public M3UPlaylist(Map<Integer, Collection<String>> services, String baseURL) {
        this.services = Map.copyOf(services);
        this.baseURL = baseURL;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Map<Integer, Collection<String>> getServices() {
        return services;
    }

    public void save(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);
        pw.println("#EXTM3U");
        for (Entry<Integer, Collection<String>> entry : services.entrySet()) {
            int freq = entry.getKey();
            for (String service : entry.getValue()) {
                pw.println("#EXTINF:0," + service);
                pw.println(String.format("%s/%s/%s", baseURL, freq,
                        URLEncoder.encode(service, StandardCharsets.UTF_8) + ".ts"));
            }
        }
        pw.flush();
    }

}

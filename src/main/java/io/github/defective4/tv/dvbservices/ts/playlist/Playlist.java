package io.github.defective4.tv.dvbservices.ts.playlist;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

public abstract class Playlist {
    private final String baseURL;
    private final Map<Integer, Collection<String>> services;

    protected Playlist(Map<Integer, Collection<String>> services, String baseURL) {
        this.baseURL = baseURL;
        this.services = Map.copyOf(services);
    }

    public String format(int freq, String service) {
        return String.format("%s/stream/%s/%s", getBaseURL(), freq,
                URLEncoder.encode(service, StandardCharsets.UTF_8) + ".ts");
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Map<Integer, Collection<String>> getServices() {
        return services;
    }

    public abstract String save(String title) throws IOException;
}

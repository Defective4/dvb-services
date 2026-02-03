package io.github.defective4.tv.dvbservices.playlist;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import io.github.defective4.tv.dvbservices.http.model.TVService;
import io.github.defective4.tv.dvbservices.media.MediaFormat;

public abstract class Playlist {
    private final String baseURL;
    private final Map<Integer, Collection<TVService>> services;

    protected Playlist(Map<Integer, Collection<TVService>> services, String baseURL) {
        this.baseURL = baseURL;
        this.services = Map.copyOf(services);
    }

    public String format(int freq, String service, MediaFormat format) {
        return String.format("%s/stream/%s/%s", getBaseURL(), freq,
                URLEncoder.encode(service, StandardCharsets.UTF_8).replace("+", "%20") + "."
                        + format.name().toLowerCase());
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Map<Integer, Collection<TVService>> getServices() {
        return services;
    }

    public abstract String save(String title, MediaFormat format) throws IOException;
}

package io.github.defective4.tv.dvbservices.ts.playlist;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

public abstract class Playlist {
    private final String baseURL;
    private final Map<Integer, Collection<String>> services;

    public Playlist(Map<Integer, Collection<String>> services, String baseURL) {
        this.baseURL = baseURL;
        this.services = Map.copyOf(services);
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Map<Integer, Collection<String>> getServices() {
        return services;
    }

    public abstract void save(Writer writer) throws IOException;
}

package io.github.defective4.tv.dvbservices;

import java.util.List;
import java.util.Map;

public class ServerSettings {
    private final List<AdapterInfo> adapters;

    public ServerSettings(List<AdapterInfo> adapters) {
        this.adapters = adapters;
    }

    public List<AdapterInfo> getAdapters() {
        return adapters;
    }

    public void validate() {

    }

    public static ServerSettings getDefaultSettings() {
        List<AdapterInfo> adapters = List.of(new AdapterInfo("file", Map.of(), 538e6f, "/tmp/tv.ts"));
        return new ServerSettings(adapters);
    }

}

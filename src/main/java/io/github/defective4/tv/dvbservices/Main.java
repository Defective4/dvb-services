package io.github.defective4.tv.dvbservices;

import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.http.DVBServer;

public class Main {
    public static void main(String[] args) {
        try {
            List<AdapterInfo> adapters = List.of(new AdapterInfo("file", Map.of(), 538e6f, "/tmp/tv.ts"));
            new DVBServer(adapters).start("0.0.0.0", 8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package io.github.defective4.tv.dvbservices.settings;

import java.io.File;
import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.AdapterInfo;

public class ServerSettings {

    public static class Cache {
        public String cacheDirectory = "cache";
        public int cacheTTL = 86400;
        public boolean enableMetadataCache = true;

        public File getCacheDirectory() {
            return new File(cacheDirectory);
        }

    }

    public static class Metadata {
        public int epgCaptureTimeout = 30;
        public int epgRefreshIntervalMinutes = 1440;
        public boolean serveM3UPlaylist = true;
        public boolean serveXMLTV = true;
        public boolean serveXSPFPlaylist = true;
    }

    public static class Server {
        public String baseURL = "http://127.0.0.1:8080";
        public boolean enableAPIEndpoint = true;
        public boolean serveMP3 = false;
        public boolean serveVideo = true;
    }

    public static class Tools {
        public String ffmpegPath = "ffmpeg";
        public String tspPath = "tsp";
    }

    public final List<AdapterInfo> adapters = List.of(new AdapterInfo("file", Map.of(), 538e6f, "/tmp/tv.ts"));
    public Cache cache = new Cache();
    public Metadata metadata = new Metadata();
    public Server server = new Server();
    public Tools tools = new Tools();

    public List<AdapterInfo> getAdapters() {
        return adapters == null ? List.of() : adapters;
    }
}

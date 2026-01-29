package io.github.defective4.tv.dvbservices.settings;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.defective4.tv.dvbservices.AdapterInfo;

public class ServerSettings {

    public static class API {
        public boolean enable = true;
        public boolean protectReadEndpoints = false;
        public boolean protectWriteEndpoints = true;
        public boolean readOnly = true;
        public String token = "CHANGEME";
    }

    public static class Cache {
        public String cacheDirectory = "cache";
        public int cacheTTL = 86400;
        public boolean enableMetadataCache = true;

        public File getCacheDirectory() {
            return new File(cacheDirectory);
        }

    }

    public static class Metadata {
        public static class Playlists {
            public boolean serveM3UPlaylist = true;
            public boolean serveXSPFPlaylist = true;
        }

        public int epgCaptureTimeout = 30;
        public int epgRefreshIntervalMinutes = 1440;
        public Playlists playlists = new Playlists();
        public boolean serveXMLTV = true;
    }

    public static class Server {
        public static class Audio {
            public String ffmpegOpts = "-aq 2";
            public boolean serveMP3 = true;
            public boolean serveWAV = false;
        }

        public static class Bind {
            public String host = "127.0.0.1";
            public int port = 8080;
        }

        public static class Video {
            public boolean serveTS = true;
        }

        public Audio audio = new Audio();
        public String baseURL = "http://127.0.0.1:8080";
        public Bind bind = new Bind();
        public Video video = new Video();
    }

    public static class Tools {
        public String ffmpegPath = "ffmpeg";
        public String tspPath = "tsp";
    }

    public final List<AdapterInfo> adapters = List.of(new AdapterInfo("file", Map.of(), 538e6f, "/tmp/tv.ts"));
    public API api = new API();
    public Cache cache = new Cache();
    public Metadata metadata = new Metadata();
    public Server server = new Server();
    public Tools tools = new Tools();

    public List<AdapterInfo> getAdapters() {
        return adapters == null ? List.of() : adapters;
    }
}

package io.github.defective4.tv.dvbservices.settings;

import java.io.File;
import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.AdapterInfo;

public class ServerSettings {

    public static class Cache {
        private String cacheDirectory = "cache";
        private int cacheTTL = 86400;
        private boolean enableMetadataCache = true;

        public File getCacheDirectory() {
            return new File(cacheDirectory);
        }

        public int getCacheTTL() {
            return cacheTTL;
        }

        public boolean isEnableMetadataCache() {
            return enableMetadataCache;
        }

        public void setCacheDirectory(File cacheDirectory) {
            this.cacheDirectory = cacheDirectory.getPath();
        }

        public void setCacheTTL(int cacheTTL) {
            this.cacheTTL = cacheTTL;
        }

        public void setEnableMetadataCache(boolean enableMetadataCache) {
            this.enableMetadataCache = enableMetadataCache;
        }

    }

    public static class Metadata {
        private int epgCaptureTimeout = 30;
        private int epgRefreshIntervalMinutes = 1440;
        private boolean serverM3UPlaylist = true;
        private boolean serveXMLTV = true;
        private boolean serveXSPFPlaylist = true;

        public int getEpgCaptureTimeout() {
            return epgCaptureTimeout;
        }

        public int getEpgRefreshIntervalMinutes() {
            return epgRefreshIntervalMinutes;
        }

        public boolean isServeEPG() {
            return serveXMLTV;
        }

        public boolean isServerM3UPlaylist() {
            return serverM3UPlaylist;
        }

        public boolean isServeXMLTV() {
            return serveXMLTV;
        }

        public boolean isServeXSPFPlaylist() {
            return serveXSPFPlaylist;
        }

        public void setEpgCaptureTimeout(int epgCaptureTimeout) {
            this.epgCaptureTimeout = epgCaptureTimeout;
        }

        public void setEpgRefreshIntervalMinutes(int epgRefreshIntervalMinutes) {
            this.epgRefreshIntervalMinutes = epgRefreshIntervalMinutes;
        }

        public void setServerM3UPlaylist(boolean serverM3UPlaylist) {
            this.serverM3UPlaylist = serverM3UPlaylist;
        }

        public void setServeXMLTV(boolean serveXMLTV) {
            this.serveXMLTV = serveXMLTV;
        }

        public void setServeXSPFPlaylist(boolean serveXSPFPlaylist) {
            this.serveXSPFPlaylist = serveXSPFPlaylist;
        }

    }

    public static class Server {
        private String baseURL = "http://127.0.0.1:8080";
        private boolean serveMP3 = false;
        private boolean serveVideo = true;

        public String getBaseURL() {
            return baseURL;
        }

        public boolean isServeMP3() {
            return serveMP3;
        }

        public boolean isServeVideo() {
            return serveVideo;
        }

        public void setBaseURL(String baseURL) {
            this.baseURL = baseURL;
        }

        public void setServeMP3(boolean serveMP3) {
            this.serveMP3 = serveMP3;
        }

        public void setServeVideo(boolean serveVideo) {
            this.serveVideo = serveVideo;
        }

    }

    public static class Tools {
        private String ffmpegPath = "ffmpeg";
        private String tspPath = "tsp";

        public String getFfmpegPath() {
            return ffmpegPath;
        }

        public String getFFmpegPath() {
            return ffmpegPath;
        }

        public String getTspPath() {
            return tspPath;
        }

        public void setFfmpegPath(String ffmpegPath) {
            this.ffmpegPath = ffmpegPath;
        }

        public void setTspPath(String tspPath) {
            this.tspPath = tspPath;
        }

    }

    private final List<AdapterInfo> adapters = List.of(new AdapterInfo("file", Map.of(), 538e6f, "/tmp/tv.ts"));
    private Cache cache = new Cache();
    private Metadata metadata = new Metadata();
    private Server server = new Server();
    private Tools tools = new Tools();

    public List<AdapterInfo> getAdapters() {
        return adapters == null ? List.of() : adapters;
    }

    public Cache getCache() {
        return cache;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Server getServer() {
        return server;
    }

    public Tools getTools() {
        return tools;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setTools(Tools tools) {
        this.tools = tools;
    }

}

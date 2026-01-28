package io.github.defective4.tv.dvbservices.settings;

import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.AdapterInfo;

public class ServerSettings {

    public static class Metadata {
        private boolean serverM3UPlaylist = true;
        private boolean serveXMLTV = true;
        private boolean serveXSPFPlaylist = true;

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
    private Metadata metadata = new Metadata();
    private Server server = new Server();
    private Tools tools = new Tools();

    public List<AdapterInfo> getAdapters() {
        return adapters == null ? List.of() : adapters;
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

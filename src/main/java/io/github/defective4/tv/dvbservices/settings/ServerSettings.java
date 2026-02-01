package io.github.defective4.tv.dvbservices.settings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.ts.playlist.PlaylistType;

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
        public static class Playlist {
            public MediaFormat format = MediaFormat.TS;
            public String name = "tv";
            public String title = "TV Playlist";
            public PlaylistType type = PlaylistType.M3U;

            public Playlist() {}

            public Playlist(PlaylistType type) {
                this.type = type;
                name += "." + type.name().toLowerCase();
            }
        }

        public int metaCaptureIntervalMinutes = 1440;
        public int metaCaptureTimeout = 30;
        public List<Playlist> playlists = List.of(new Playlist(PlaylistType.M3U), new Playlist(PlaylistType.XSPF));
        public boolean scheduleMetaCapture = true;
        public boolean serveXMLTV = true;
    }

    public static class Server {
        public static class Audio {
            public String converterParams = "";
        }

        public static class Bind {
            public String host = "0.0.0.0";
            public int port = 8080;
        }

        public Audio audio = new Audio();
        public String baseURL = "http://127.0.0.1:8080";
        public Bind bind = new Bind();
        public List<MediaFormat> formats = Arrays.asList(MediaFormat.values());

        public boolean needsTranscoding() {
            return formats.stream().anyMatch(fmt -> fmt != MediaFormat.TS);
        }
    }

    public static class Tools {
        public static enum ConverterType {
            FFMPEG, VLC;
        }

        public static class Paths {
            public String ffmpegPath = "ffmpeg";
            public String tspPath = "tsp";
            public String vlcPath = "vlc";
        }

        public static enum StreamProviderType {
            TSDUCK, VLC;
        }

        public ConverterType mediaConverter = ConverterType.FFMPEG;
        public StreamProviderType metadataProvider = StreamProviderType.TSDUCK;
        public Paths paths = new Paths();
        public StreamProviderType streamProvider = StreamProviderType.TSDUCK;
    }

    public final List<AdapterInfo> adapters = List.of(new AdapterInfo(null, new AdapterOptions("dvb", null,
            Map.of("delivery-system", "dvb-t2", "frequency", "538000000"), new String[0]), 538e6f));
    public API api = new API();
    public Cache cache = new Cache();
    public Metadata metadata = new Metadata();
    public Server server = new Server();
    public Tools tools = new Tools();

    public List<AdapterInfo> getAdapters() {
        return adapters == null ? List.of() : adapters;
    }
}

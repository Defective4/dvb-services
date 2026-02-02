package io.github.defective4.tv.dvbservices.ts.playlist;

import io.javalin.http.ContentType;

public enum MediaFormat {
    MP3("mp3", new String[0], false, "mp3", ContentType.AUDIO_MPEG.getMimeType()),
    OGG("vorb", new String[0], true, "ogg", "audio/ogg"),
    OPUS("opus", new String[] { "-ac", "2" }, false, null, ContentType.AUDIO_OPUS.getMimeType()),
    TS(null, null, false, null, "video/mp2t"),
    WAV("s16l", new String[0], false, null, ContentType.AUDIO_WAV.getMimeType());

    private final String acodec;
    private final String[] ffmpegArgs;
    private final String mime;
    private final String mux;
    private final boolean requiresIntermediate;

    private MediaFormat(String acodec, String[] ffmpegArgs, boolean requiresIntermediate, String mux, String mime) {
        this.acodec = acodec;
        this.mux = mux;
        this.ffmpegArgs = ffmpegArgs;
        this.requiresIntermediate = requiresIntermediate;
        this.mime = mime;
    }

    public String getAcodec() {
        return acodec;
    }

    public String[] getFFmpegArgs() {
        return ffmpegArgs;
    }

    public String getMime() {
        return mime;
    }

    public String getMux() {
        return mux;
    }

    public boolean isVideo() {
        return this == TS;
    }

    public boolean requiresIntermediate() {
        return requiresIntermediate;
    }
}
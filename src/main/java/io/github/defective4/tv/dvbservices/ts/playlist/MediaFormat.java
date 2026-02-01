package io.github.defective4.tv.dvbservices.ts.playlist;

public enum MediaFormat {
    MP3("mp3", new String[0], false, "mp3"), OGG("vorb", new String[0], true, "ogg"),
    OPUS("opus", new String[] { "-ac", "2" }, false, "raw"), TS(null, null, false, null),
    WAV("s16l", new String[0], false, "wav");

    private final String acodec;
    private final String[] ffmpegArgs;
    private final String mux;
    private final boolean requiresIntermediate;

    private MediaFormat(String acodec, String[] ffmpegArgs, boolean requiresIntermediate, String mux) {
        this.acodec = acodec;
        this.mux = mux;
        this.ffmpegArgs = ffmpegArgs;
        this.requiresIntermediate = requiresIntermediate;
    }

    public String getAcodec() {
        return acodec;
    }

    public String[] getFFmpegArgs() {
        return ffmpegArgs;
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
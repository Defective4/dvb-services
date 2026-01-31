package io.github.defective4.tv.dvbservices.ts.playlist;

public enum MediaFormat {
    MP3, OGG(true), OPUS(new String[] { "-ac", "2" }), TS, WAV;

    private final String[] ffmpegArgs;
    private final boolean requiresIntermediate;

    private MediaFormat() {
        this(new String[0], false);
    }

    private MediaFormat(boolean requiresIntermediate) {
        this(new String[0], requiresIntermediate);
    }

    private MediaFormat(String[] ffmpegArgs) {
        this(ffmpegArgs, false);
    }

    private MediaFormat(String[] ffmpegArgs, boolean requiresIntermediate) {
        this.ffmpegArgs = ffmpegArgs;
        this.requiresIntermediate = requiresIntermediate;
    }

    public String[] getFFmpegArgs() {
        return ffmpegArgs;
    }

    public boolean isVideo() {
        return this == TS;
    }

    public boolean requiresIntermediate() {
        return requiresIntermediate;
    }
}
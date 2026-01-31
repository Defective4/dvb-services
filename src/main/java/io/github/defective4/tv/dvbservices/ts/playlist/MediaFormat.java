package io.github.defective4.tv.dvbservices.ts.playlist;

public enum MediaFormat {
    MP3, TS, WAV, OGG, OPUS;

    public boolean isVideo() {
        return this == TS;
    }
}
package io.github.defective4.tv.dvbservices.media;

public interface MediaConverterFactory<T extends MediaConverter> {
    T create();
}

package io.github.defective4.tv.dvbservices.ts;

public interface TransportStreamProviderFactory<T extends TransportStreamProvider> {
    T create();
}

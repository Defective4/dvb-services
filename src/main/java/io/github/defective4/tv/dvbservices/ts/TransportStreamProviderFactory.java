package io.github.defective4.tv.dvbservices.ts;

public abstract class TransportStreamProviderFactory<T extends TransportStreamProvider> {
    public abstract T create();
}

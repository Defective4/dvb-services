package io.github.defective4.tv.dvbservices.ts;

public interface ProviderFactory<T extends Provider> {
    T create();
}

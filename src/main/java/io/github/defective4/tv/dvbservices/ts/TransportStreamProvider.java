package io.github.defective4.tv.dvbservices.ts;

import java.io.IOException;
import java.io.InputStream;

import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;

public interface TransportStreamProvider extends Provider {
    InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException;
}

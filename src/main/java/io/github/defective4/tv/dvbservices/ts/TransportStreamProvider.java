package io.github.defective4.tv.dvbservices.ts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import io.github.defective4.tv.dvbservices.AdapterInfo;

public interface TransportStreamProvider extends AutoCloseable {

    InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException;

    @Override
    void close() throws IOException;

    void dumpPSI(AdapterInfo adapter, File target, long timeout) throws IOException;

    String getFullName();

    boolean isAvailable() throws IOException;
}

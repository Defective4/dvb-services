package io.github.defective4.tv.dvbservices.ts.external;

import java.io.File;
import java.io.IOException;

public interface TransportStreamProvider extends AutoCloseable {
    @Override
    void close() throws IOException;

    void dumpPSI(int frequency, File target, long timeout) throws IOException;
}

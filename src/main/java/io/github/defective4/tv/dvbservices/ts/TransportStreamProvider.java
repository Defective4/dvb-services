package io.github.defective4.tv.dvbservices.ts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import io.github.defective4.tv.dvbservices.AdapterInfo;

public abstract class TransportStreamProvider implements AutoCloseable {

    protected TransportStreamProvider() {}

    public abstract InputStream captureTS(AdapterInfo adapter) throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract void dumpPSI(AdapterInfo adapter, File target, File patOutput, File sdtOutput, long timeout)
            throws IOException;
}

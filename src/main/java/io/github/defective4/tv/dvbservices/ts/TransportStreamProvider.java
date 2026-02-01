package io.github.defective4.tv.dvbservices.ts;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public interface TransportStreamProvider extends AutoCloseable {

    MetadataResult captureMetadata(AdapterInfo adapter, long timeout)
            throws IOException, NotAnMPEGFileException, ParseException;

    InputStream captureTS(AdapterInfo adapter, int service, boolean audioOnly) throws IOException;

    @Override
    void close() throws IOException;

    String getFullName();

    boolean isAvailable() throws IOException;
}

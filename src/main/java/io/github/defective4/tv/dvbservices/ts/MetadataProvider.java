package io.github.defective4.tv.dvbservices.ts;

import java.io.IOException;
import java.text.ParseException;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public interface MetadataProvider extends Provider {
    MetadataResult captureMetadata(AdapterInfo adapter, long timeout)
            throws IOException, NotAnMPEGFileException, ParseException;
}

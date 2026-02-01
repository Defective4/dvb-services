package io.github.defective4.tv.dvbservices.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;

public interface MediaConverter extends AutoCloseable {

    @Override
    void close() throws IOException;

    void closePeacefully() throws InterruptedException;

    void convert(InputStream from, OutputStream to, MediaFormat fmt, String... opts)
            throws IOException, InterruptedException, ExecutionException;

    boolean isAvailable() throws IOException;

}

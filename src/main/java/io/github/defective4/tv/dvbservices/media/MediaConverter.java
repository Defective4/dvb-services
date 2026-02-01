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

    String getName();

    boolean isAvailable() throws IOException;

    boolean isFormatSupported(MediaFormat fmt);

    public static void copyStream(InputStream from, OutputStream fo) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while (true) {
            read = from.read(buffer);
            if (read < 0) break;
            fo.write(buffer, 0, read);
            fo.flush();
        }
    }

}

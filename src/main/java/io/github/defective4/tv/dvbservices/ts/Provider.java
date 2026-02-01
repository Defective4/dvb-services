package io.github.defective4.tv.dvbservices.ts;

import java.io.IOException;

public interface Provider extends AutoCloseable {

    @Override
    void close() throws IOException;

    String getFullName();

    boolean isAvailable() throws IOException;
}

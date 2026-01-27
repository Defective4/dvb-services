package io.github.defective4.tv.dvbservices.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TemporaryFiles {
    private TemporaryFiles() {}

    public static File getTemporaryFile(String suffix) throws IOException {
        File file = Files.createTempFile("dvbsvc", "." + suffix).toFile();
        file.deleteOnExit();
        return file;
    }
}

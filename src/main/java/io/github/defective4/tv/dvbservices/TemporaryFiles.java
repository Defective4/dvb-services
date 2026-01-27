package io.github.defective4.tv.dvbservices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TemporaryFiles {
    private TemporaryFiles() {}

    public static File getTemporaryDir() throws IOException {
        File dir = Files.createTempDirectory("dvbsvc").toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> remove(dir)));
        return dir;
    }

    public static File getTemporaryFile(String suffix) throws IOException {
        File file = Files.createTempFile("dvbsvc", "." + suffix).toFile();
        file.deleteOnExit();
        return file;
    }

    private static void remove(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) remove(f);
        }
        dir.delete();
    }
}

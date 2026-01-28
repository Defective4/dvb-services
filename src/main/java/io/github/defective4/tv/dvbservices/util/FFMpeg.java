package io.github.defective4.tv.dvbservices.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FFMpeg implements AutoCloseable {
    private final String ffmpegPath;
    private Process process;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public FFMpeg(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public void close() {
        service.shutdownNow();
        if (process != null) process.destroyForcibly();
    }

    public void closePeacefully() throws InterruptedException {
        process.waitFor();
    }

    public void convertToMP3(InputStream from, OutputStream to)
            throws IOException, InterruptedException, ExecutionException {
        if (process != null) throw new IllegalStateException("Converter already started");
        process = ProcessUtils.start(ffmpegPath, "-i", "-", "-f", "mp3", "-");

        Future<Boolean> task = service.submit(() -> {
            byte[] buffer = new byte[1024];
            int read;
            try (InputStream fi = process.getInputStream()) {
                while (true) {
                    read = fi.read(buffer);
                    if (read < 0) break;
                    to.write(buffer, 0, read);
                }
            } catch (IOException e) {}
            return true;
        });

        byte[] buffer = new byte[1024];
        int read;
        try (OutputStream fo = process.getOutputStream()) {
            while (true) {
                read = from.read(buffer);
                if (read < 0) break;
                fo.write(buffer, 0, read);
            }
        } catch (IOException e) {}

        task.get();
    }

}

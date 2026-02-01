package io.github.defective4.tv.dvbservices.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;

public class FFMpeg implements AutoCloseable {

    private static final String VERESION_STRING = "ffmpeg version ";
    private final String ffmpegPath;
    private Process process;
    private final ExecutorService service = Executors.newFixedThreadPool(2);

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

    public void convert(InputStream from, OutputStream to, MediaFormat fmt, String... opts)
            throws IOException, InterruptedException, ExecutionException {
        if (process != null) throw new IllegalStateException("Converter already started");

        List<String> args = new ArrayList<>(List.of(ffmpegPath, "-i", "-", "-f", fmt.name().toLowerCase()));
        if (opts != null && opts.length > 0) Collections.addAll(args, opts);
        Collections.addAll(args, fmt.getFFmpegArgs());
        args.add("-");

        process = ProcessUtils.start(args.toArray(new String[0]));

        Future<Boolean> task = service.submit(() -> {
            try (InputStream fi = process.getInputStream()) {
                copyStream(fi, to);
            } catch (IOException e) {}
            return true;
        });

        if (fmt.requiresIntermediate()) {
            Process wav = ProcessUtils.start(ffmpegPath, "-i", "-", "-f", "wav", "-");
            Future<Boolean> subtask = service.submit(() -> {
                try (OutputStream so = wav.getOutputStream()) {
                    copyStream(from, so);
                } catch (IOException e) {}
                return true;
            });
            try (OutputStream fo = process.getOutputStream()) {
                copyStream(wav.getInputStream(), fo);
            } catch (IOException e) {}
            subtask.get();
        } else {
            try (OutputStream fo = process.getOutputStream()) {
                copyStream(from, fo);
            } catch (IOException e) {}
        }

        task.get();
    }

    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(ffmpegPath, "-version");
        try (BufferedReader reader = proc.inputReader()) {
            String line = reader.readLine();
            return line != null && line.startsWith(VERESION_STRING);
        }
    }

    private static void copyStream(InputStream from, OutputStream fo) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while (true) {
            read = from.read(buffer);
            if (read < 0) break;
            fo.write(buffer, 0, read);
        }
    }

}

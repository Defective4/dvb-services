package io.github.defective4.tv.dvbservices.media;

import static io.github.defective4.tv.dvbservices.media.MediaConverter.copyStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.util.ProcessUtils;

public class VLC implements MediaConverter {

    private Process process;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final String vlcPath;

    private VLC(String vlcPath) {
        this.vlcPath = vlcPath;
    }

    @Override
    public void close() throws IOException {
        service.shutdownNow();
        if (process != null) process.destroyForcibly();
    }

    @Override
    public void closePeacefully() throws InterruptedException {}

    @Override
    public void convert(InputStream from, OutputStream to, MediaFormat fmt, String... opts)
            throws IOException, InterruptedException, ExecutionException {
        if (process != null) throw new IllegalStateException("Converter already started");
        process = ProcessUtils.start(vlcPath, "-I", "dummy", "--sout", String.format(
                "#transcode{vcodec=none,acodec=%s,channels=2,samplerate=44100,scodec=none%s}:file{mux=%s,dst=/dev/stdout}",
                fmt.getAcodec(), opts.length == 0 ? "" : "," + String.join(" ", opts), fmt.getMux()), "--no-sout-all",
                "--sout-keep", "-");

        service.submit(() -> {
            try (OutputStream fo = process.getOutputStream()) {
                copyStream(from, fo);
            } catch (IOException e) {}
            return true;
        });

        try (InputStream fi = process.getInputStream()) {
            copyStream(fi, to);
        } catch (IOException e) {}
    }

    @Override
    public String getName() {
        return "VLC";
    }

    @Override
    public boolean isAvailable() throws IOException {
        Process proc = ProcessUtils.start(vlcPath, "--version");
        try (BufferedReader reader = proc.errorReader()) {
            String line = reader.readLine();
            return line == null;
        }
    }

    @Override
    public boolean isFormatSupported(MediaFormat fmt) {
        return fmt.getMux() != null;
    }

    public static MediaConverterFactory<VLC> factory(String vlcPath) {
        return () -> new VLC(vlcPath);
    }

}

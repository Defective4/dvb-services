package io.github.defective4.tv.dvbservices.ts.external;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TSDuck implements AutoCloseable {

    private Process process;
    private final String tspExecutable;

    public TSDuck(String tspExecutable) {
        this.tspExecutable = tspExecutable;
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    public void dumpPSI(int frequency, File output, long timeout) throws IOException {
        String[] args = { tspExecutable, "-I", "dvb", "--delivery-system", "dvb-t2", "--frequency",
                Integer.toString(frequency), "-P", "filter", "--psi-si", "-O", "file", output.getPath() };
        process = new ProcessBuilder(args).start();
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
    }
}

package io.github.defective4.tv.dvbservices.ts.external;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TSDuck implements AutoCloseable {

    private static final String TSP_EXECUTABLE = "tsp";
    private Process process;

    @Override
    public void close() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    public void dumpPSI(int frequency, File output, long timeout) throws IOException {
//        String[] args = { TSP_EXECUTABLE, "-I", "dvb", "--delivery-system", "dvb-t2", "--frequency",
//                Integer.toString(frequency), "-P", "filter", "--psi-si", "-O", "file", output.getPath() };
        String[] args = { TSP_EXECUTABLE, "-I", "file", "/tmp/tv.ts", "-P", "filter", "--psi-si", "-O", "file",
                output.getPath() };
        process = new ProcessBuilder(args).start();
        try {
            process.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
    }
}

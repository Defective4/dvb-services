package io.github.defective4.tv.dvbservices.toml;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

public class TOMLWriter implements AutoCloseable {
    private final PrintWriter writer;

    public TOMLWriter(Writer writer) {
        this.writer = new PrintWriter(writer);
    }

    @Override
    public void close() {
        writer.close();
    }

    public void write(Map<String, Map<String, String>> values) {
        for (Entry<String, Map<String, String>> entry : values.entrySet()) {
            writer.println(String.format("[%s]", entry.getKey()));
            for (Entry<String, String> sub : entry.getValue().entrySet()) {
                writer.println(String.format("	%s = %s", sub.getKey(), sub.getValue()));
            }
        }
    }
}

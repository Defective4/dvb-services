package io.github.defective4.tv.dvbservices.toml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TOMLReader implements AutoCloseable {

    private final BufferedReader reader;

    public TOMLReader(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public Map<String, Map<String, String>> read() throws IOException {
        String lastSection = null;
        Map<String, Map<String, String>> values = new LinkedHashMap<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
            if (line.startsWith("[") && line.endsWith("]")) {
                lastSection = line.substring(1, line.length() - 1);
            } else if (lastSection != null) {
                String[] split = line.split("=");
                if (split.length < 2) continue;
                String key = split[0].trim();
                String value = String.join(" ", Arrays.copyOfRange(split, 1, split.length)).trim();

                values.computeIfAbsent(lastSection, t -> new LinkedHashMap<>()).put(key, value);
            }
        }
        return Collections.unmodifiableMap(values);
    }
}

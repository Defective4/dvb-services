package io.github.defective4.tv.dvbservices;

import java.util.Map;

public record AdapterInfo(String driver, Map<String, String> options, float frequency, String... args) {
    public int freq() {
        return (int) frequency;
    }

    @Override
    public Map<String, String> options() {
        return options == null ? Map.of() : options;
    }

    @Override
    public String driver() {
        return driver == null ? "file" : driver;
    }

    @Override
    public String[] args() {
        return args == null ? new String[0] : args;
    }

}

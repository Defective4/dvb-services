package io.github.defective4.tv.dvbservices.http.model;

import java.util.Map;

public record AdapterOptions(String driver, String input, Map<String, String> options, String[] args) {
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
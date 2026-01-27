package io.github.defective4.tv.dvbservices;

import java.util.Map;

public record AdapterInfo(String driver, Map<String, String> options, float frequency, String... args) {
    public int freq() {
        return (int) frequency;
    }
}

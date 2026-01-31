package io.github.defective4.tv.dvbservices;

import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.tv.dvbservices.util.HashUtil;

public record AdapterInfo(String driver, Map<String, String> options, float frequency, String... args) {
    public int freq() {
        return (int) frequency;
    }

    public String calculateString() {
        StringBuilder builder = new StringBuilder();
        builder.append(driver()).append("_").append(freq()).append("_");
        StringBuilder opsBuilder = new StringBuilder();
        for (Entry<String, String> entry : options().entrySet())
            opsBuilder.append(entry.getKey() + "_" + entry.getValue());

        for (String arg : args()) opsBuilder.append(arg);

        builder.append(HashUtil.hash(opsBuilder.toString()));
        return builder.toString();
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

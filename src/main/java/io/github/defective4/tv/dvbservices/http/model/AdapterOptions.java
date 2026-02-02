package io.github.defective4.tv.dvbservices.http.model;

import java.util.HashMap;
import java.util.Map;

public record AdapterOptions(String driver, Map<String, String> options, String[] args) {
    @Override
    public Map<String, String> options() {
        return options == null ? Map.of() : options;
    }

    public AdapterOptions merge(AdapterOptions ops) {
        Map<String, String> mergedOps = new HashMap<>(options);
        mergedOps.putAll(ops.options());
        String[] mergedArgs = join(args, ops.args());
        return new AdapterOptions(driver(), mergedOps, mergedArgs);
    }

    private static String[] join(String[] arr1, String[] arr2) {
        String[] joined = new String[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, joined, 0, arr1.length);
        System.arraycopy(arr2, 0, joined, arr1.length, arr2.length);
        return joined;
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
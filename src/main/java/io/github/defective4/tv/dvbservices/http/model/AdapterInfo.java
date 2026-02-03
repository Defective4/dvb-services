package io.github.defective4.tv.dvbservices.http.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.tv.dvbservices.util.HashUtil;

public record AdapterInfo(AdapterOptions metadataCaptureOptions, AdapterOptions streamOptions, float frequency) {

    @Override
    public AdapterOptions metadataCaptureOptions() {
        return metadataCaptureOptions == null ? streamOptions() : metadataCaptureOptions;
    }

    @Override
    public AdapterOptions streamOptions() {
        return streamOptions == null ? new AdapterOptions(null, Map.of(), new String[0]) : streamOptions;
    }

    public AdapterInfo merge(AdapterInfo info) {
        return new AdapterInfo(metadataCaptureOptions.merge(info.metadataCaptureOptions),
                streamOptions.merge(info.streamOptions), frequency);
    }

    public int freq() {
        return (int) frequency;
    }

    public String calculateString() {
        StringBuilder builder = new StringBuilder();
        builder.append(freq()).append("_");
        StringBuilder opsBuilder = new StringBuilder();
        for (AdapterOptions ops : List.of(streamOptions(), metadataCaptureOptions())) {
            for (Entry<String, String> entry : ops.options().entrySet())
                opsBuilder.append(entry.getKey() + "_" + entry.getValue());

            for (String arg : ops.args()) opsBuilder.append(arg);
        }

        builder.append(HashUtil.hash(opsBuilder.toString()));
        return builder.toString();
    }

}

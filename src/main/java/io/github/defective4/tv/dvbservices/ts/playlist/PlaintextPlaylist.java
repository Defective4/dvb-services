package io.github.defective4.tv.dvbservices.ts.playlist;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class PlaintextPlaylist extends Playlist {

    public PlaintextPlaylist(Map<Integer, Collection<String>> services, String baseURL) {
        super(services, baseURL);
    }

    @Override
    public String save(String title) throws IOException {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
            for (Entry<Integer, Collection<String>> entry : getServices().entrySet()) {
                entry.getValue().forEach(svc -> pw.println(format(entry.getKey(), svc)));
            }
        }
        return writer.toString();
    }

}

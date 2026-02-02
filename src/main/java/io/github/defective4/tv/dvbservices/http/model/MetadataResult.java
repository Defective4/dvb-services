package io.github.defective4.tv.dvbservices.http.model;

import java.util.List;
import java.util.Map;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;

public record MetadataResult(Map<Integer, List<FriendlyEvent>> events, Map<Integer, String> services) {

}

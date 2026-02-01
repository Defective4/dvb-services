package io.github.defective4.tv.dvbservices.http.model;

import java.util.Collection;
import java.util.Map;

public record APIServices(Map<Integer, Collection<TVService>> services) {
}

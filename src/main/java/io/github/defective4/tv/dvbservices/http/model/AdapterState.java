package io.github.defective4.tv.dvbservices.http.model;

public enum AdapterState {
    AVAILABLE, CAPTURING_EPG, WATCHING;

    public boolean isAvailable() {
        return this == AVAILABLE;
    }
}

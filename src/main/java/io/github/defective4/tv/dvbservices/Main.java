package io.github.defective4.tv.dvbservices;

import io.github.defective4.tv.dvbservices.http.DVBServer;

public class Main {
    public static void main(String[] args) {
        try {
            new DVBServer().start("0.0.0.0", 8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

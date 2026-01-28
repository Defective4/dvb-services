package io.github.defective4.tv.dvbservices;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.defective4.tv.dvbservices.http.DVBServer;

public class Main {
    public static void main(String[] args) {
        try {
            File settingsFile = new File("settings.json");
            ServerSettings settings;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            if (!settingsFile.exists()) {
                try (Writer writer = new FileWriter(settingsFile, StandardCharsets.UTF_8)) {
                    gson.toJson(ServerSettings.getDefaultSettings(), writer);
                    System.err.println("Review " + settingsFile + ", and start the server again");
                }
                System.exit(1);
                return;
            }

            try (Reader reader = new FileReader(settingsFile, StandardCharsets.UTF_8)) {
                settings = gson.fromJson(reader, ServerSettings.class);
            }

            new DVBServer(settings).start("0.0.0.0", 8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

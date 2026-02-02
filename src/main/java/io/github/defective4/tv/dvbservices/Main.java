package io.github.defective4.tv.dvbservices;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.defective4.tv.dvbservices.http.DVBServer;
import io.github.defective4.tv.dvbservices.settings.ServerSettings;
import io.github.defective4.tv.dvbservices.settings.SettingsGenerator;

public class Main {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = new SimpleLoggerFactory().getLogger("main");
    private static final Options OPTIONS = new Options()
            .addOption(Option.builder("h").desc("Display this help page").longOpt("help").build())
            .addOption(Option.builder("c").desc("Use a different settings file").longOpt("config").hasArg()
                    .argName("file").build())
            .addOption(Option.builder("g").desc(
                    "Generate a new settings file. If used with -c the generated file will be saved to specified location.")
                    .longOpt("generate-config").build());

    private static File SETTINGS_FILE = new File("settings.json");

    public static void main(String[] args) {
        try {
            CommandLine cli;
            try {
                cli = new DefaultParser().parse(OPTIONS, args);
            } catch (Exception e) {
                printHelp(e);
                System.exit(2);
                return;
            }

            if (cli.hasOption('h')) {
                printHelp(null);
                return;
            }

            if (cli.hasOption('c')) {
                SETTINGS_FILE = new File(cli.getOptionValue('c'));
            }

            if (cli.hasOption('g')) {
                LOGGER.info("Starting interactive settings generation");
                ServerSettings settings = new SettingsGenerator().startInteractiveSetup();
                try (Writer writer = new FileWriter(SETTINGS_FILE)) {
                    GSON.toJson(settings, writer);
                    LOGGER.info("Settings saved to " + SETTINGS_FILE.getPath());
                }
                return;
            }

            runApp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printHelp(Throwable error) {
        String name = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName();
        HelpFormatter fmt = new HelpFormatter();
        fmt.printHelp(name + " <options> [args...]", error == null ? null : error.getMessage(), OPTIONS, null);
    }

    private static void runApp() throws IOException {
        ServerSettings settings;

        if (!SETTINGS_FILE.exists()) {
            LOGGER.error(
                    "The settings file {} is missing. Generate a new one with the {} option, or point to an existing one with {}",
                    SETTINGS_FILE.getPath(), "-g", "-c <file>");
            System.exit(1);
            return;
        }

        try (Reader reader = new FileReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
            settings = GSON.fromJson(reader, ServerSettings.class);
        }

        new DVBServer(settings).start(settings.server.bind.host, settings.server.bind.port);
    }
}

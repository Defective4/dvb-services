package io.github.defective4.tv.dvbservices.settings;

import static io.github.defective4.tv.dvbservices.cli.CLIValidators.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.defective4.tv.dvbservices.cli.CLIValidators;
import io.github.defective4.tv.dvbservices.cli.ChoiceValidator;
import io.github.defective4.tv.dvbservices.cli.CommandLineInput;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Metadata.Playlist;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.ConverterType;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.ProviderType;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.ts.TransportStreamProvider;
import io.github.defective4.tv.dvbservices.ts.playlist.MediaFormat;
import io.github.defective4.tv.dvbservices.ts.playlist.PlaylistType;

public class SettingsGenerator {
    private final CommandLineInput cli = new CommandLineInput();

    public CommandLineInput getCli() {
        return cli;
    }

    public ServerSettings startInteractiveSetup(ServerSettings existing) {
        System.err.println();
        ServerSettings defaults = existing == null ? new ServerSettings() : existing;

        boolean save = false;

        if (existing != null) {
            boolean edit = true;
            while (edit) {
                List<AdapterInfo> adapters = existing.getAdapters();
                StringBuilder builder = new StringBuilder();
                builder.append("There are ").append(adapters.size()).append(" adapters in the file:").append("\n");
                for (int i = 0; i < adapters.size(); i++) {
                    builder.append(" ").append(i).append(". ").append(adapters.get(i)).append("\n");
                }
                builder.append("\n").append("What do you want to do?");
                switch (cli.ask(
                        new ChoiceValidator(
                                Map.of('r', "(R)emove", 'a', "(A)dd", 's', "(S)ave", 'b', "A(b)ort", 'c', "(C)lear")),
                        null, builder.toString())) {
                    case 'c': {
                        adapters.clear();
                        System.err.println("Adapter list cleared");
                        System.err.println();
                        break;
                    }
                    case 'r': {
                        if (adapters.isEmpty()) {
                            System.err.println("There are no adapters to remove");
                        } else {
                            adapters.remove((int) cli.ask(integer(0, adapters.size() - 1), null,
                                    "Which adapter do you want to remove?"));
                        }
                        System.err.println();
                        break;
                    }
                    case 's': {
                        edit = false;
                        save = true;
                        break;
                    }
                    case 'a': {
                        edit = false;
                        break;
                    }
                    case 'b':
                    default: {
                        System.err.println("Aborted");
                        System.exit(0);
                        return existing;
                    }
                }
            }
        }

        switch (existing != null ? 'd'
                : cli.ask(new ChoiceValidator(Map.of('e', "(E)verything", 'd', "A(d)apters", 'a', "(A)bort")), null,
                        "What do you want to configure?")) {
            case 'e': {
                // Bind settings
                defaults.server.bind.host = cli.ask(defaults.server.bind.host, "Server bind address");
                defaults.server.bind.port = cli.ask(integer(1, Short.MAX_VALUE), defaults.server.bind.port,
                        "Server bind port");
                defaults.server.baseURL = cli.ask(CLIValidators.URL, defaults.server.baseURL,
                        "Server base URL used for access from outside.").toString();

                // Metadata settings
                defaults.metadata.scheduleMetaCapture = cli.ask(BOOL, defaults.metadata.scheduleMetaCapture,
                        "Enable metadata capturing schedule?");
                if (defaults.metadata.scheduleMetaCapture) {
                    defaults.metadata.metaCaptureIntervalMinutes = cli.ask(integer(1, Integer.MAX_VALUE),
                            defaults.metadata.metaCaptureIntervalMinutes, "Metadata capture interval (minutes)");
                    defaults.metadata.metaCaptureTimeout = cli.ask(integer(1, Integer.MAX_VALUE),
                            defaults.metadata.metaCaptureTimeout, "Metadata capture timeout (seconds)");
                }

                defaults.metadata.serveXMLTV = cli.ask(BOOL, defaults.metadata.serveXMLTV,
                        "Enable serving of XMLTV data under /meta/epg.xml?");

                Playlist defaultPlaylist = new Playlist();
                boolean add = false;
                List<Playlist> playlists = new ArrayList<>();
                while (true) {
                    add = cli.ask(BOOL, null, String.format("Do you want to add %s playlist?", add ? "another" : "a"));
                    if (add) {
                        Playlist ps = new Playlist();
                        ps.type = cli.ask(enumeration(PlaylistType.class), null,
                                "Playlist type\n" + "Valid types: " + Arrays.toString(PlaylistType.values()));
                        ps.name = cli.ask(defaultPlaylist.name + "." + ps.type.name().toLowerCase(),
                                "Playlist name.\nAfter adding, the playlist will be available under /playlist/{name}");
                        ps.title = cli.ask(defaultPlaylist.title, "Playlist title");
                        ps.format = cli.ask(enumeration(MediaFormat.class), null, "Media format of the playlist\n"
                                + "Valid formats: " + Arrays.toString(MediaFormat.values()));
                        playlists.add(ps);
                    } else
                        break;
                }

                defaults.metadata.playlists = Collections.unmodifiableList(playlists);
                System.err.println("Added " + playlists.size() + " playlists");

                // Cache settings

                defaults.cache.enableMetadataCache = cli.ask(BOOL, null, "Do you want to enable metadata cache?\n"
                        + "This will help you avoid scanning for EPG and TV service data on each server startup");

                if (defaults.cache.enableMetadataCache) {
                    defaults.cache.cacheTTL = cli.ask(integer(0, Integer.MAX_VALUE), defaults.cache.cacheTTL,
                            "Metadata cache validity (in seconds)");
                    defaults.cache.cacheDirectory = cli.ask(defaults.cache.cacheDirectory, "Cache directory");
                }

                // API

                defaults.api.enable = cli.ask(BOOL, null, "Enable API endpoints?");
                if (defaults.api.enable) {
                    defaults.api.readOnly = cli.ask(BOOL, null, "Make the API read-only?\n"
                            + "If enabled, all endpoints used to alter server state in any way will be disabled.");
                    defaults.api.protectReadEndpoints = cli.ask(BOOL, null,
                            "If enabled, all API read endpoints will locked behind a bearer token.");
                    defaults.api.protectWriteEndpoints = cli.ask(BOOL, null,
                            "If enabled, all API write endpoints will locked behind a bearer token.");
                    defaults.api.token = cli.ask(null,
                            "Provide a password that will be used to access protected API endpoints");
                }

                // Media converting

                defaults.server.formats = cli.ask(list(enumeration(MediaFormat.class)), null,
                        "Comma-separated list of media formats enabled on this server.\n"
                                + "Please note, that all non-TS formats will require transcoding.\n"
                                + "Available values: " + Arrays.toString(MediaFormat.values()));

                if (defaults.server.needsTranscoding()) {
                    defaults.tools.mediaConverter = cli.ask(enumeration(ConverterType.class), null,
                            "Some of the selected formats require transcoding.\n"
                                    + "Choose a media converter you'd like to use.\n"
                                    + "Note, that the selected converter binary has to be installed in your system.\n"
                                    + "Available values: " + Arrays.toString(ConverterType.values()));
                    String params = cli.ask("-aq 2",
                            "Additional command line parameters to pass to the audio converter.", true);
                    if (!params.equalsIgnoreCase("none")) defaults.server.audio.converterParams = params;
                }

                // Provider settings

                defaults.tools.streamProvider = cli.ask(t -> {
                    ProviderType type = enumeration(ProviderType.class).apply(t);
                    if (!type.isOfType(TransportStreamProvider.class)) {
                        throw new IllegalArgumentException(t + " can't be used as a stream provider");
                    }
                    return type;
                }, null, "Select a stream provider.\n"
                        + "It will be used to capture and broadcast live tv signal to the users.\n"
                        + "Available values: "
                        + Arrays.toString(Arrays.stream(ProviderType.values())
                                .filter(type -> type.isOfType(TransportStreamProvider.class))
                                .toArray(ProviderType[]::new)));

                defaults.tools.metadataProvider = cli.ask(t -> {
                    ProviderType type = enumeration(ProviderType.class).apply(t);
                    if (!type.isOfType(MetadataProvider.class)) {
                        throw new IllegalArgumentException(t + " can't be used as a metadata provider");
                    }
                    return type;
                }, null, "Select a metadata provider.\n"
                        + "It will be used to capture TV metadata, such as service list and EPG.\n"
                        + "Available values: " + Arrays.toString(Arrays.stream(ProviderType.values())
                                .filter(type -> type.isOfType(MetadataProvider.class)).toArray(ProviderType[]::new)));

            }
            case 'd': {
                boolean add = true;
                List<AdapterInfo> adapters = new ArrayList<>(defaults.getAdapters());

                if (!save) switch (cli.ask(new ChoiceValidator(Map.of('y', "(Y)es", 'n', "(N)o", 'q', "(Q)uick setup")),
                        null, "Do you want to setup adapters?")) {
                    case 'q': {
                        List<Integer> frequencies = cli.ask(list(FREQUENCY), "538M, 562M",
                                "Comma-separated list of adapter frequencies (Hz)");
                        String tsDriver = cli.ask("dvb-t2", "Stream provider delivery system");
                        String[] tsArgs = cli.ask(list(STRING), null, "Additional driver arguments", true)
                                .toArray(new String[0]);
                        Map<String, String> tsOps = new HashMap<>(cli.ask(map(STRING),
                                "delivery-system=dvb-t2, frequency=538000000", "Additional driver options", true));

                        String metaDriver = cli.ask("dvb-t2", "Metadata provider delivery system");
                        String[] metaArgs = cli.ask(list(STRING), null, "Additional driver arguments", true)
                                .toArray(new String[0]);
                        Map<String, String> metaOps = new HashMap<>(cli.ask(map(STRING),
                                "delivery-system=dvb-t2, frequency=538000000", "Additional driver options", true));

                        for (int freq : frequencies) {
                            AdapterOptions metaBase = defaults.tools.metadataProvider.getInfoGenerator()
                                    .apply(freq, metaDriver).merge(new AdapterOptions(metaDriver, metaOps, metaArgs));
                            AdapterOptions tsBase = defaults.tools.streamProvider.getInfoGenerator()
                                    .apply(freq, tsDriver).merge(new AdapterOptions(tsDriver, tsOps, tsArgs));
                            adapters.add(new AdapterInfo(metaBase, tsBase, freq));
                        }
                        break;
                    }
                    case 'y': {
                        while (add) {
                            int freq = cli.ask(FREQUENCY, "538000000, 538e6, 538M", "Adapter frequency (Hz)");

                            adapters.add(constructAdapterInfo(defaults, freq));
                            add = cli.ask(BOOL, null, String.format("Do you want to add another adapter?"));
                        }
                        break;
                    }
                    case 'n':
                    default: {
                        break;
                    }
                }

                defaults.adapters = Collections.unmodifiableList(adapters);
                break;
            }
            case 'a':
            default: {
                System.err.println("Aborted");
                System.exit(1);
                break;
            }
        }
        return defaults;
    }

    private AdapterInfo constructAdapterInfo(ServerSettings defaults, int freq) {
        String tsDriver = cli.ask("dvb-t2", "Stream provider delivery system");
        String[] tsArgs = cli.ask(list(STRING), null, "Additional driver arguments", true).toArray(new String[0]);
        Map<String, String> tsOps = new HashMap<>(
                cli.ask(map(STRING), "delivery-system=dvb-t2, frequency=538000000", "Additional driver options", true));

        String metaDriver = cli.ask("dvb-t2", "Metadata provider delivery system");
        String[] metaArgs = cli.ask(list(STRING), null, "Additional driver arguments", true).toArray(new String[0]);
        Map<String, String> metaOps = new HashMap<>(
                cli.ask(map(STRING), "delivery-system=dvb-t2, frequency=538000000", "Additional driver options", true));

        AdapterOptions metaBase = defaults.tools.metadataProvider.getInfoGenerator().apply(freq, metaDriver)
                .merge(new AdapterOptions(metaDriver, metaOps, metaArgs));
        AdapterOptions tsBase = defaults.tools.streamProvider.getInfoGenerator().apply(freq, tsDriver)
                .merge(new AdapterOptions(tsDriver, tsOps, tsArgs));

        return new AdapterInfo(metaBase, tsBase, freq);
    }

    private static String[] join(String[] arr1, String[] arr2) {
        String[] joined = new String[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, joined, 0, arr1.length);
        System.arraycopy(arr2, 0, joined, arr1.length, arr2.length);
        return joined;
    }
}

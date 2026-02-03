package io.github.defective4.tv.dvbservices.ts.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import io.github.defective4.tv.dvbservices.epg.FriendlyEvent;
import io.github.defective4.tv.dvbservices.http.model.AdapterInfo;
import io.github.defective4.tv.dvbservices.http.model.AdapterOptions;
import io.github.defective4.tv.dvbservices.http.model.MetadataResult;
import io.github.defective4.tv.dvbservices.settings.ServerSettings.Tools.Paths;
import io.github.defective4.tv.dvbservices.ts.MetadataProvider;
import io.github.defective4.tv.dvbservices.ts.ProviderFactory;
import nl.digitalekabeltelevisie.gui.exception.NotAnMPEGFileException;

public class TestMetadataProvider implements MetadataProvider {

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final String EPG_DAYS_KEY = "epgDays";
    private static final String EPG_DAYS_VALUE = "1";
    private static final Random RANDOM = new Random();
    private static final String SEED_KEY = "seed";
    private static final String SERVICES_KEY = "services";

    private static final String SERVICES_VALUE = "5";

    private TestMetadataProvider() {}

    @Override
    public MetadataResult captureMetadata(AdapterInfo adapter, long timeout)
            throws IOException, NotAnMPEGFileException, ParseException {
        int maxServices, epgDays;
        long seed;
        try {
            Map<String, String> ops = adapter.metadataCaptureOptions().options();
            maxServices = Integer.parseInt(ops.getOrDefault(SERVICES_KEY, SERVICES_VALUE));
            epgDays = Integer.parseInt(ops.getOrDefault(EPG_DAYS_KEY, EPG_DAYS_VALUE));
            seed = Long.parseLong(ops.getOrDefault(SEED_KEY, Long.toString(RANDOM.nextLong())));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }

        Random random = new Random(seed);
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0,
                0, 0);

        long midnight = calendar.getTimeInMillis();
        long until = midnight + TimeUnit.DAYS.toMillis(epgDays);

        Map<Integer, String> services = new HashMap<>();
        Map<Integer, List<FriendlyEvent>> epg = new HashMap<>();
        for (int i = 0; i < maxServices; i++) {
            String channelName = generateString(random, 16);
            services.put(i, channelName);
            long start = midnight;
            List<FriendlyEvent> events = new ArrayList<>();
            while (start < until) {
                String name = generateString(random, 16);
                int len = (random.nextInt(24) + 1) * 5;
                StringBuilder builder = new StringBuilder();

                int words = random.nextInt(50);
                for (int j = 0; j < words; j++) {
                    builder.append(generateString(random, random.nextInt(10) + 1));
                    if (random.nextInt(5) == 0) builder.append(".");
                    builder.append(" ");
                }

                events.add(new FriendlyEvent(name, builder.toString().trim(), List.of(), List.of(), len, List.of(),
                        new Date(start)));
                start += TimeUnit.MINUTES.toMillis(len);
            }
            epg.put(i, Collections.unmodifiableList(events));
        }

        return new MetadataResult(epg, services);
    }

    @Override
    public void close() throws IOException {}

    @Override
    public String getFullName() {
        return "TEST";
    }

    @Override
    public boolean isAvailable() throws IOException {
        return true;
    }

    public static ProviderFactory<TestMetadataProvider> factory(Paths paths) {
        return () -> new TestMetadataProvider();
    }

    public static BiFunction<Integer, String, AdapterOptions> infoGenerator() {
        return (freq, ds) -> new AdapterOptions("test", Map.of(SERVICES_KEY, SERVICES_VALUE, SEED_KEY,
                Long.toString(RANDOM.nextLong()), EPG_DAYS_KEY, EPG_DAYS_VALUE), new String[0]);
    }

    private static String generateString(Random rand, int len) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) builder.append(CHARS[rand.nextInt(CHARS.length)]);
        return builder.toString();
    }

}

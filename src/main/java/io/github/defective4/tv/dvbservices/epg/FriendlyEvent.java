package io.github.defective4.tv.dvbservices.epg;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.digitalekabeltelevisie.controller.DVBString;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.ContentDescriptor;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.Descriptor;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.ExtendedEventDescriptor;
import nl.digitalekabeltelevisie.data.mpeg.descriptors.ParentalRatingDescriptor;
import nl.digitalekabeltelevisie.data.mpeg.psi.EITsection.Event;
import nl.digitalekabeltelevisie.util.Utils;

public class FriendlyEvent {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private final List<Integer> ageRatings;
    private final String description;
    private final List<String> genres;
    private final List<String> keywords;
    private final long minutes;
    private final String name;
    private final Date start;

    public FriendlyEvent(String name, String description, List<String> genres, List<String> keywords, long minutes,
            List<Integer> ageRatings, Date start) {
        this.name = name;
        this.description = description;
        this.genres = Collections.unmodifiableList(genres);
        this.keywords = Collections.unmodifiableList(keywords);
        this.minutes = minutes;
        this.ageRatings = Collections.unmodifiableList(ageRatings);
        this.start = start;
    }

    public List<Integer> getAgeRatings() {
        return ageRatings;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getGenres() {
        return genres;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public long getMinutes() {
        return minutes;
    }

    public String getName() {
        return name;
    }

    public Date getStart() {
        return start;
    }

    public static FriendlyEvent fromEvent(Event event) throws ParseException {
        String name = event.getEventName();
        Date start = FORMAT.parse(Utils.getEITStartTimeAsString(event.getStartTime()));
        List<DVBString> dvbStrings = new ArrayList<>();
        Descriptor.findGenericDescriptorsInList(event.getDescriptorList(), ExtendedEventDescriptor.class)
                .forEach(desc -> dvbStrings.add(desc.getText()));
        List<String> keywords = new ArrayList<>();
        List<String> genres = new ArrayList<>();

        long minutes = Utils.getDurationSeconds(event.getDuration()) / 60;

        Descriptor.findGenericDescriptorsInList(event.getDescriptorList(), ContentDescriptor.class)
                .forEach(desc -> desc.getContentList().forEach(item -> {
                    String part1 = ContentDescriptor.getContentNibbleLevel1String(item.contentNibbleLevel1());
                    String part2 = ContentDescriptor.getContentNibbleLevel2String(item.contentNibbleLevel1(),
                            item.contentNibbleLevel2());
                    Collections.addAll(keywords, part2.split("/"));
                    Collections.addAll(genres, part1.replace(":", "").split("/"));
                }));
        
        String description = String.join("", dvbStrings.stream().map(DVBString::toString).toArray(String[]::new));
        List<Integer> ageRatings = new ArrayList<>();

        Descriptor.findGenericDescriptorsInList(event.getDescriptorList(), ParentalRatingDescriptor.class)
                .forEach(desc -> desc.getRatingList().forEach(rating -> {
                    int type = rating.getRating();
                    if (1 <= type && type <= 15) ageRatings.add(type + 3);
                }));

        return new FriendlyEvent(name, description, genres, keywords, minutes, ageRatings, start);
    }

    public static DateFormat getFormat() {
        return FORMAT;
    }
}

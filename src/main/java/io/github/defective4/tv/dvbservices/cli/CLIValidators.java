package io.github.defective4.tv.dvbservices.cli;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CLIValidators {

    public static final Function<String, Boolean> BOOL = in -> {
        char c = Character.toLowerCase(in.charAt(0));
        if (c == 'y' || c == 't')
            return true;
        else if (c == 'n' || c == 'f') return false;
        throw new IllegalArgumentException(
                "Illegal boolean value. Accepted values are [y,t,true] for True, or [n,f,false] for False");
    };

    public static final Function<String, Integer> FREQUENCY = in -> {
        int val;
        int multiplier = 1;
        String str = in.toLowerCase();
        try {
            if (str.endsWith("m")) {
                multiplier = 1000000;
                str = str.substring(0, str.length() - 1);
            }
            val = (int) Float.parseFloat(str);
            if (val < 0) throw new IllegalArgumentException("Frequency can't be negative");
            if (val * multiplier < 1e6f) throw new IllegalArgumentException("Frequency is too low");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(in + " is not a valid frequency");
        }

        return val * multiplier;
    };

    public static final Function<String, String> STRING = input -> input;

    public static final Function<String, URL> URL = in -> {
        try {
            while (in.endsWith("/")) in = in.substring(0, in.length() - 1);
            return URI.create(in).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The provided URL is invalid");
        }
    };

    private CLIValidators() {}

    public static <E extends Enum<E>> Function<String, E> enumeration(Class<E> type) {
        return in -> {
            try {
                return Enum.valueOf(type, in.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(in + " is not a valid value");
            }
        };
    }

    public static Function<String, Integer> integer(int min, int max) {
        return in -> {
            try {
                int val = Integer.parseInt(in);
                if (val < min) throw new IllegalArgumentException("The value can't be lower than " + min);
                if (val > max) throw new IllegalArgumentException("The value can't be higher than " + max);
                return val;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(in + " is not a valid int value");
            }
        };
    }

    public static <E> Function<String, List<E>> list(Function<String, E> validator) {
        return in -> {
            if (in.isBlank()) return List.of();
            String[] parts = in.split(",");
            if (Arrays.stream(parts).filter(t -> !t.isBlank()).count() == 0)
                throw new IllegalArgumentException("The list is empty");
            return Arrays.stream(parts).filter(t -> !t.isBlank()).map(part -> validator.apply(part.trim())).toList();
        };
    }

    public static <E> Function<String, Map<String, E>> map(Function<String, E> validator) {
        return in -> {
            if (in.isBlank()) return Map.of();
            Map<String, E> map = new HashMap<>();
            String[] pairs = in.split(",");
            for (String pair : pairs) {
                String[] split = pair.trim().split("=");
                if (split.length > 1) {
                    String key = split[0];
                    String value = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
                    map.put(key, validator.apply(value));
                } else
                    throw new IllegalArgumentException("The map contains invalid elements");
            }
            return Collections.unmodifiableMap(map);
        };
    }
}

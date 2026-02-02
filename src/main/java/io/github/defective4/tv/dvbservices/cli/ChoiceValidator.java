package io.github.defective4.tv.dvbservices.cli;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class ChoiceValidator implements Function<String, Character> {

    private final Map<Character, String> choices;

    public ChoiceValidator(Map<Character, String> choices) {
        this.choices = Collections.unmodifiableMap(choices);
    }

    @Override
    public Character apply(String t) {
        char c = Character.toLowerCase(t.charAt(0));
        if (!choices.containsKey(c)) throw new IllegalArgumentException("Invalid choice");
        return c;
    }

    public Map<Character, String> getChoices() {
        return choices;
    }

}

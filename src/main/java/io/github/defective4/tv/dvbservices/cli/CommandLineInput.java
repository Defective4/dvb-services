package io.github.defective4.tv.dvbservices.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

public class CommandLineInput {
    private final BufferedReader cliReader = new BufferedReader(new InputStreamReader(System.in));

    public <R> R ask(Function<String, R> validator, Object example, String prompt) {
        return ask(validator, example, prompt, false);
    }

    public <R> R ask(Function<String, R> validator, Object example, String prompt, boolean allowEmpty) {
        while (true) {
            for (String line : prompt.split("\n")) System.err.println(" " + line);
            if (example != null) System.err.println(" Example: " + example);
            if (validator == CLIValidators.BOOL)
                System.err.print("[y/n] ");
            else {
                if (validator instanceof ChoiceValidator choice)
                    System.err.println(String.join("/", choice.getChoices().values().toArray(new String[0])));
                System.err.print("> ");
            }
            String response;
            try {
                response = cliReader.readLine();
                System.err.println();
                if (response.isBlank() && !allowEmpty) {
                    System.err.println("ERROR: Response can't be empty");
                    continue;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            try {
                return validator.apply(response);
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message == null) message = String.format("\"%s\" is invalid", response);
                System.err.println("ERROR: " + message);
            }
        }
    }

    public String ask(Object example, String prompt) {
        return ask(CLIValidators.STRING, example, prompt);
    }

    public String ask(Object example, String prompt, boolean allowEmpty) {
        return ask(CLIValidators.STRING, example, prompt, allowEmpty);
    }
}

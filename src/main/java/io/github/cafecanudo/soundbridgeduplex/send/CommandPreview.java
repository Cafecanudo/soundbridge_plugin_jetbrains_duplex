package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CommandPreview {

    private static final String QUOTE = "\"";
    private static final Set<String> ALWAYS_QUOTED = Set.of("--tx", "--dest");

    private CommandPreview() {
    }

    public static String format(List<String> command) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < command.size(); i++) {
            String argument = command.get(i);
            boolean pathValue = i > 0 && ALWAYS_QUOTED.contains(command.get(i - 1));
            parts.add(pathValue || needsQuotes(argument) ? quote(argument) : argument);
        }
        return String.join(" ", parts);
    }

    private static boolean needsQuotes(String argument) {
        return argument.isEmpty() || argument.chars().anyMatch(Character::isWhitespace);
    }

    private static String quote(String argument) {
        return QUOTE + argument.replace(QUOTE, "\\" + QUOTE) + QUOTE;
    }
}

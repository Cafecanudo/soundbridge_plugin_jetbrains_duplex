package io.github.cafecanudo.soundbridgeduplex.ignore;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record IgnorePattern(String entry, boolean anchored, Pattern regex) {

    private static final String SEPARATOR = "/";
    private static final String PARENT = "..";
    private static final String GLOB_CHARS = "*?";
    private static final String REGEX_META = "\\.[]{}()+-^$|";
    private static final Pattern INPUT_SEPARATORS = Pattern.compile("[\\r\\n,;]+");

    public static boolean isPattern(String entry) {
        return entry.chars().anyMatch(c -> GLOB_CHARS.indexOf(c) >= 0);
    }

    public static Optional<IgnorePattern> of(String entry) {
        if (!isPattern(entry)) {
            return Optional.empty();
        }
        boolean anchored = entry.startsWith(SEPARATOR);
        String body = anchored ? entry.substring(1) : entry;
        return Optional.of(new IgnorePattern(entry, anchored, Pattern.compile(toRegex(body))));
    }

    public static String normalize(String input) {
        String entry = input.strip().replace('\\', '/');
        while (entry.endsWith(SEPARATOR) && entry.length() > 1) {
            entry = entry.substring(0, entry.length() - 1);
        }
        if (!isPattern(entry)) {
            while (entry.startsWith(SEPARATOR)) {
                entry = entry.substring(1);
            }
        }
        return entry;
    }

    public static Optional<String> problemKey(String entry) {
        if (entry.isEmpty() || entry.equals(SEPARATOR)) {
            return Optional.of("ignore.add.empty");
        }
        if (List.of(entry.split(SEPARATOR)).contains(PARENT)) {
            return Optional.of("ignore.add.parent");
        }
        if (!isPattern(entry)) {
            try {
                Path.of(entry);
            } catch (InvalidPathException e) {
                return Optional.of("ignore.add.invalid");
            }
        }
        return Optional.empty();
    }

    public static List<String> split(String input) {
        Set<String> entries = new LinkedHashSet<>();
        for (String part : INPUT_SEPARATORS.split(input == null ? "" : input)) {
            if (!part.isBlank()) {
                entries.add(normalize(part));
            }
        }
        return new ArrayList<>(entries);
    }

    public boolean matches(List<String> relativeParts) {
        int lastStart = anchored ? 0 : relativeParts.size() - 1;
        for (int start = 0; start <= lastStart; start++) {
            for (int end = start + 1; end <= relativeParts.size(); end++) {
                if (regex.matcher(String.join(SEPARATOR, relativeParts.subList(start, end))).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String toRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*' && glob.startsWith("**/", i)) {
                regex.append("(?:.*/)?");
                i += 3;
            } else if (c == '*' && glob.startsWith("**", i)) {
                regex.append(".*");
                i += 2;
            } else if (c == '*') {
                regex.append("[^/]*");
                i++;
            } else if (c == '?') {
                regex.append("[^/]");
                i++;
            } else {
                if (REGEX_META.indexOf(c) >= 0) {
                    regex.append('\\');
                }
                regex.append(c);
                i++;
            }
        }
        return regex.toString();
    }
}

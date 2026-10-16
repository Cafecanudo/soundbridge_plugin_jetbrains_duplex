package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SendSelection(List<Path> sends, List<Path> deletions) {

    public static final SendSelection EMPTY = new SendSelection(List.of(), List.of());

    public SendSelection {
        sends = List.copyOf(sends);
        deletions = List.copyOf(deletions);
    }

    public static SendSelection of(List<Path> sends) {
        return new SendSelection(sends, List.of());
    }

    public static SendSelection fromChanges(List<PathChange> changes, List<Path> files) {
        Map<String, Path> sends = new LinkedHashMap<>();
        Map<String, Path> deletions = new LinkedHashMap<>();
        for (PathChange change : changes) {
            if (change.after() != null) {
                sends.putIfAbsent(change.after().toString(), change.after());
            }
            if (change.before() != null && !sameText(change.before(), change.after())) {
                deletions.putIfAbsent(change.before().toString(), change.before());
            }
        }
        files.forEach(file -> sends.putIfAbsent(file.toString(), file));
        sends.keySet().forEach(deletions::remove);
        return new SendSelection(List.copyOf(sends.values()), List.copyOf(deletions.values()));
    }

    public boolean isEmpty() {
        return sends.isEmpty() && deletions.isEmpty();
    }

    public boolean hasDeletions() {
        return !deletions.isEmpty();
    }

    private static boolean sameText(Path before, Path after) {
        return after != null && before.toString().equals(after.toString());
    }
}

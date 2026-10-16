package io.github.cafecanudo.soundbridgeduplex.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HistoryLog {

    public static final int MAX_ENTRIES = 200;

    private HistoryLog() {
    }

    public static List<String> add(List<String> stored, HistoryEntry entry) {
        List<String> updated = new ArrayList<>();
        updated.add(entry.toJson());
        updated.addAll(stored.subList(0, Math.min(stored.size(), MAX_ENTRIES - 1)));
        return updated;
    }

    public static List<String> replace(List<String> stored, HistoryEntry old, HistoryEntry entry) {
        List<String> updated = new ArrayList<>(stored);
        for (int i = 0; i < updated.size(); i++) {
            if (HistoryEntry.fromJson(updated.get(i)).filter(old::equals).isPresent()) {
                updated.set(i, entry.toJson());
                return updated;
            }
        }
        return add(stored, entry);
    }

    public static List<HistoryEntry> read(List<String> stored) {
        return stored.stream().map(HistoryEntry::fromJson).flatMap(Optional::stream).toList();
    }
}

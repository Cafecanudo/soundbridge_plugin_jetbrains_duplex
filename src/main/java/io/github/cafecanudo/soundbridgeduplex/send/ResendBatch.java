package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyStatus;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ResendBatch(SendSelection selection, SendSnapshot snapshot) {

    public static ResendBatch of(List<HistoryEntry> entries, Path base, boolean onlyProblems) {
        Set<Path> sends = new LinkedHashSet<>();
        Set<Path> deletions = new LinkedHashSet<>();
        for (HistoryEntry entry : entries) {
            if (entry.direction().isVerify()) {
                verifiedPaths(entry, onlyProblems).forEach(path -> sends.add(resolve(base, path)));
            } else if (!onlyProblems) {
                entry.items().forEach(item -> sends.add(resolve(base, item)));
                entry.deleted().forEach(item -> deletions.add(resolve(base, item)));
            }
        }
        SendSnapshot snapshot = entries.size() == 1 ? entries.get(0).snapshot() : null;
        return new ResendBatch(new SendSelection(List.copyOf(sends), List.copyOf(deletions)), snapshot);
    }

    public static ResendBatch ofPaths(List<String> paths, Path base) {
        List<Path> sends = paths.stream().map(path -> resolve(base, path)).distinct().toList();
        return new ResendBatch(new SendSelection(sends, List.of()), null);
    }

    public static boolean hasProblems(HistoryEntry entry) {
        return entry.direction().isVerify()
                && entry.verified().stream().anyMatch(item -> item.status() != VerifyStatus.EXISTS);
    }

    public boolean isEmpty() {
        return selection.isEmpty();
    }

    private static List<String> verifiedPaths(HistoryEntry entry, boolean onlyProblems) {
        if (!onlyProblems) {
            return entry.items();
        }
        return entry.verified().stream()
                .filter(item -> item.status() != VerifyStatus.EXISTS)
                .map(VerifyItem::path)
                .toList();
    }

    private static Path resolve(Path base, String item) {
        Path path = Path.of(item);
        return path.isAbsolute() ? path : base.resolve(path);
    }
}

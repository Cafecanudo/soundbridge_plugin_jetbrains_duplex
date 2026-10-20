package io.github.cafecanudo.soundbridgeduplex.ignore;

import io.github.cafecanudo.soundbridgeduplex.send.SendFiles;
import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class IgnoreFilter {

    public record Result(SendSelection kept, List<Path> ignoredSends, List<Path> ignoredDeletions) {

        public Result {
            ignoredSends = List.copyOf(ignoredSends);
            ignoredDeletions = List.copyOf(ignoredDeletions);
        }

        public boolean hasIgnored() {
            return !ignoredSends.isEmpty() || !ignoredDeletions.isEmpty();
        }

        public int ignoredCount() {
            return ignoredSends.size() + ignoredDeletions.size();
        }
    }

    private IgnoreFilter() {
    }

    public static Result apply(SendSelection selection, IgnoreList ignore, Predicate<Path> isDirectory) {
        List<Path> sends = new ArrayList<>();
        List<Path> ignoredSends = new ArrayList<>();
        for (Path item : selection.sends()) {
            if (ignore.isIgnored(item)) {
                ignoredSends.add(item);
            } else if (isDirectory.test(item) && ignore.hasIgnoredUnder(item)) {
                split(SendFiles.expand(List.of(item)), ignore, sends, ignoredSends);
            } else {
                sends.add(item);
            }
        }
        List<Path> deletions = new ArrayList<>();
        List<Path> ignoredDeletions = new ArrayList<>();
        split(selection.deletions(), ignore, deletions, ignoredDeletions);
        return new Result(new SendSelection(sends, deletions), ignoredSends, ignoredDeletions);
    }

    private static void split(List<Path> items, IgnoreList ignore, List<Path> kept, List<Path> ignored) {
        for (Path item : items) {
            (ignore.isIgnored(item) ? ignored : kept).add(item);
        }
    }
}

package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class DestinationResolver {

    private static final String SEPARATOR = "/";

    private DestinationResolver() {
    }

    public static String defaultDestination(Path projectBase, List<Path> items, Predicate<Path> isDirectory) {
        if (projectBase == null || items.isEmpty()) {
            return "";
        }
        Path base = projectBase.toAbsolutePath().normalize();
        Optional<Path> common = items.stream()
                .map(item -> item.toAbsolutePath().normalize())
                .map(item -> isDirectory.test(item) ? item : item.getParent())
                .map(Optional::ofNullable)
                .reduce(DestinationResolver::commonAncestor)
                .orElse(Optional.empty());
        return common
                .filter(folder -> folder.startsWith(base) && !folder.equals(base))
                .map(folder -> toDestination(base.relativize(folder)))
                .orElse("");
    }

    private static Optional<Path> commonAncestor(Optional<Path> first, Optional<Path> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return Optional.empty();
        }
        Path candidate = first.get();
        while (candidate != null && !second.get().startsWith(candidate)) {
            candidate = candidate.getParent();
        }
        return Optional.ofNullable(candidate);
    }

    private static String toDestination(Path relative) {
        return relative.toString().replace(relative.getFileSystem().getSeparator(), SEPARATOR);
    }
}

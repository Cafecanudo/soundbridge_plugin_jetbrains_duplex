package io.github.cafecanudo.soundbridgeduplex.verify;

import io.github.cafecanudo.soundbridgeduplex.send.SendFiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record VerifyPlan(List<String> paths, List<Path> outside) {

    public static final int DEFAULT_BATCH_CHARS = 24_000;
    public static final String ITEM_SEPARATOR = ";";

    private static final String PATH_SEPARATOR = "/";

    public VerifyPlan {
        paths = List.copyOf(paths);
        outside = List.copyOf(outside);
    }

    public static VerifyPlan of(Path base, List<Path> selected) {
        Path root = base.toAbsolutePath().normalize();
        Set<String> paths = new LinkedHashSet<>();
        List<Path> outside = new ArrayList<>();
        for (Path file : SendFiles.expand(selected)) {
            Path absolute = file.toAbsolutePath().normalize();
            if (absolute.startsWith(root) && !absolute.equals(root)) {
                paths.add(relative(root, absolute));
            } else {
                outside.add(file);
            }
        }
        return new VerifyPlan(List.copyOf(paths), outside);
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public List<List<String>> batches(int maxChars) {
        List<List<String>> batches = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int length = 0;
        for (String path : paths) {
            int added = current.isEmpty() ? path.length() : path.length() + ITEM_SEPARATOR.length();
            if (!current.isEmpty() && length + added > maxChars) {
                batches.add(List.copyOf(current));
                current.clear();
                added = path.length();
                length = 0;
            }
            current.add(path);
            length += added;
        }
        if (!current.isEmpty()) {
            batches.add(List.copyOf(current));
        }
        return batches;
    }

    private static String relative(Path root, Path file) {
        Path relative = root.relativize(file);
        List<String> parts = new ArrayList<>();
        relative.forEach(part -> parts.add(part.toString()));
        return String.join(PATH_SEPARATOR, parts);
    }
}

package io.github.cafecanudo.soundbridgeduplex.ignore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record IgnoreList(Path base, List<String> entries) {

    private static final String SEPARATOR = "/";

    public IgnoreList {
        base = base.toAbsolutePath().normalize();
        entries = List.copyOf(entries);
    }

    public boolean isIgnored(Path path) {
        Path target = normalize(path);
        return entries.stream().map(this::resolve).anyMatch(target::startsWith);
    }

    public boolean isEntry(Path path) {
        return entries.contains(key(path));
    }

    public boolean hasIgnoredUnder(Path folder) {
        Path root = normalize(folder);
        return entries.stream().map(this::resolve).anyMatch(entry -> entry.startsWith(root) && !entry.equals(root));
    }

    public List<String> with(List<Path> paths) {
        Set<String> result = new LinkedHashSet<>(entries);
        paths.stream().filter(path -> !isIgnored(path)).map(this::key).forEach(result::add);
        return List.copyOf(result);
    }

    public List<String> without(List<String> removed) {
        List<String> result = new ArrayList<>(entries);
        result.removeAll(removed);
        return List.copyOf(result);
    }

    public Path resolve(String entry) {
        Path path = Path.of(entry);
        return (path.isAbsolute() ? path : base.resolve(path)).normalize();
    }

    public String key(Path path) {
        Path target = normalize(path);
        Path stored = target.startsWith(base) && !target.equals(base) ? base.relativize(target) : target;
        List<String> parts = new ArrayList<>();
        if (stored.getRoot() != null) {
            parts.add(stored.getRoot().toString().replace("\\", SEPARATOR).replaceAll(SEPARATOR + "$", ""));
        }
        stored.forEach(part -> parts.add(part.toString()));
        return String.join(SEPARATOR, parts);
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}

package io.github.cafecanudo.soundbridgeduplex.ignore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public record IgnoreList(Path base, List<String> entries, List<IgnorePattern> patterns, List<Path> patternBases) {

    public static final List<String> DEFAULT_ENTRIES = List.of(
            "**/.git",
            "**/.idea", "*.iml", "*.ipr", "*.iws",
            "**/.vs", "*.suo", "*.user", "*.userosscache", "*.sln.docstates");

    private static final String SEPARATOR = "/";

    public static List<String> withDefaults(List<String> entries) {
        Set<String> result = new LinkedHashSet<>(DEFAULT_ENTRIES);
        result.addAll(entries);
        return List.copyOf(result);
    }

    public IgnoreList {
        base = base.toAbsolutePath().normalize();
        entries = List.copyOf(entries);
        patterns = List.copyOf(patterns);
        patternBases = patternBases.stream().map(IgnoreList::normalize).distinct().toList();
    }

    public IgnoreList(Path base, List<String> entries) {
        this(base, entries, null);
    }

    public IgnoreList(Path base, List<String> entries, Path receiveBase) {
        this(base, entries, entries.stream().map(IgnorePattern::of).flatMap(Optional::stream).toList(),
                patternBases(base, receiveBase));
    }

    public static List<Path> patternBases(Path base, Path receiveBase) {
        return Stream.of(base, receiveBase).filter(Objects::nonNull).map(IgnoreList::normalize).distinct().toList();
    }

    public boolean isIgnored(Path path) {
        Path target = normalize(path);
        return pathEntries().map(this::resolve).anyMatch(target::startsWith) || matchesPattern(target);
    }

    public boolean isEntry(Path path) {
        return entries.contains(key(path));
    }

    public boolean hasIgnoredUnder(Path folder) {
        Path root = normalize(folder);
        boolean entryUnder = pathEntries().map(this::resolve)
                .anyMatch(entry -> entry.startsWith(root) && !entry.equals(root));
        return entryUnder || patternUnder(root);
    }

    public List<String> with(List<Path> paths) {
        Set<String> result = new LinkedHashSet<>(entries);
        paths.stream().filter(path -> !isIgnored(path)).map(this::key).forEach(result::add);
        return List.copyOf(result);
    }

    public List<String> withEntries(List<String> added) {
        Set<String> result = new LinkedHashSet<>(entries);
        result.addAll(added);
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

    private Stream<String> pathEntries() {
        return entries.stream().filter(entry -> !IgnorePattern.isPattern(entry));
    }

    private boolean matchesPattern(Path target) {
        if (patterns.isEmpty()) {
            return false;
        }
        return patternBases.stream()
                .filter(root -> target.startsWith(root) && !target.equals(root))
                .map(root -> parts(root.relativize(target)))
                .anyMatch(parts -> patterns.stream().anyMatch(pattern -> pattern.matches(parts)));
    }

    private static List<String> parts(Path relative) {
        List<String> parts = new ArrayList<>();
        relative.forEach(part -> parts.add(part.toString()));
        return parts;
    }

    private boolean patternUnder(Path root) {
        if (patterns.isEmpty() || patternBases.stream().noneMatch(root::startsWith) || !Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.anyMatch(path -> !path.equals(root) && matchesPattern(normalize(path)));
        } catch (IOException | UncheckedIOException e) {
            return true;
        }
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}

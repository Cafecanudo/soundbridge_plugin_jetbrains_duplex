package io.github.cafecanudo.soundbridgeduplex.send;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SendFiles {

    private SendFiles() {
    }

    public static SendSelection expand(SendSelection selection) {
        return new SendSelection(expand(selection.sends()), selection.deletions());
    }

    public static SendSelection prepare(SendSelection selection, boolean expandFolders) {
        return expandFolders ? expand(selection) : selection;
    }

    public static List<Path> expand(List<Path> items) {
        Set<Path> files = new LinkedHashSet<>();
        items.forEach(item -> files.addAll(filesOf(item)));
        return List.copyOf(files);
    }

    private static List<Path> filesOf(Path item) {
        if (!Files.isDirectory(item)) {
            return List.of(item);
        }
        try (Stream<Path> walk = Files.walk(item)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException | UncheckedIOException e) {
            return List.of();
        }
    }
}

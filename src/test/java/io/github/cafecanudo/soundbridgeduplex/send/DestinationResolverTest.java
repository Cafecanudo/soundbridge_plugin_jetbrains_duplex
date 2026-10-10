package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DestinationResolverTest {

    private static final Path BASE = Path.of("C:/IdeaProjects/untitled");
    private static final Path FILE = BASE.resolve("src/sss/nnn/bbb/ddd/YTr.java");
    private static final Path FOLDER = BASE.resolve("src/sss/nnn");
    private static final Set<Path> DIRECTORIES = Set.of(FOLDER.toAbsolutePath().normalize(), BASE.toAbsolutePath().normalize());

    @Test
    void fileUsesItsFolderFromProjectRoot() {
        assertEquals("src/sss/nnn/bbb/ddd", resolve(List.of(FILE)));
    }

    @Test
    void folderUsesItsOwnPath() {
        assertEquals("src/sss/nnn", resolve(List.of(FOLDER)));
    }

    @Test
    void severalItemsUseCommonFolder() {
        assertEquals("src/sss/nnn", resolve(List.of(FILE, FOLDER)));
    }

    @Test
    void outsideProjectOrProjectRootIsEmpty() {
        assertEquals("", resolve(List.of(Path.of("D:/outro/a.txt"))));
        assertEquals("", resolve(List.of(BASE.resolve("a.txt"))));
        assertEquals("", resolve(List.of()));
    }

    private static String resolve(List<Path> items) {
        return DestinationResolver.defaultDestination(BASE, items, DIRECTORIES::contains);
    }
}

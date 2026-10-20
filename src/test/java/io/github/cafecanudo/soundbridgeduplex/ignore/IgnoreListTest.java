package io.github.cafecanudo.soundbridgeduplex.ignore;

import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreListTest {

    @TempDir
    Path root;

    @Test
    void folderEntryIgnoresEverythingBelowIt() {
        IgnoreList list = new IgnoreList(root, List.of("build", "src/Secret.java"));

        assertTrue(list.isIgnored(root.resolve("build")));
        assertTrue(list.isIgnored(root.resolve("build/libs/a.jar")));
        assertTrue(list.isIgnored(root.resolve("src/Secret.java")));
        assertFalse(list.isIgnored(root.resolve("src/Main.java")));
        assertFalse(list.isIgnored(root.resolve("buildSrc/x.kt")));
        assertTrue(list.hasIgnoredUnder(root.resolve("src")));
        assertFalse(list.hasIgnoredUnder(root.resolve("build")));
    }

    @Test
    void entriesAreRelativeInsideTheProjectAndSkipWhatIsAlreadyIgnored() {
        IgnoreList list = new IgnoreList(root, List.of("build"));

        List<String> entries = list.with(List.of(root.resolve("src/a.txt"), root.resolve("build/x"),
                root.resolve("src/a.txt")));

        assertEquals(List.of("build", "src/a.txt"), entries);
        assertTrue(new IgnoreList(root, entries).isEntry(root.resolve("src/a.txt")));
        assertEquals(List.of("src/a.txt"), new IgnoreList(root, entries).without(List.of("build")));
    }

    @Test
    void pathsOutsideTheProjectAreStoredAbsolute() {
        Path outside = root.getParent().resolve("fora/a.txt");
        IgnoreList list = new IgnoreList(root.resolve("projeto"), List.of());

        String key = list.key(outside);

        assertTrue(Path.of(key).isAbsolute());
        assertTrue(new IgnoreList(root.resolve("projeto"), List.of(key)).isIgnored(outside));
    }

    @Test
    void filterSplitsSendsAndDeletionsAndOpensFoldersWithIgnoredItems() throws IOException {
        Path src = Files.createDirectories(root.resolve("src"));
        Path main = Files.writeString(src.resolve("Main.java"), "m");
        Files.writeString(src.resolve("Secret.java"), "s");
        Path build = Files.createDirectories(root.resolve("build"));
        IgnoreList list = new IgnoreList(root, List.of("build", "src/Secret.java"));
        SendSelection selection = new SendSelection(List.of(src, build), List.of(root.resolve("build/old.jar"),
                root.resolve("velho.txt")));

        IgnoreFilter.Result result = IgnoreFilter.apply(selection, list, Files::isDirectory);

        assertEquals(List.of(main), result.kept().sends());
        assertEquals(List.of(root.resolve("velho.txt")), result.kept().deletions());
        assertEquals(List.of(src.resolve("Secret.java"), build), result.ignoredSends());
        assertEquals(List.of(root.resolve("build/old.jar")), result.ignoredDeletions());
        assertEquals(3, result.ignoredCount());
    }
}

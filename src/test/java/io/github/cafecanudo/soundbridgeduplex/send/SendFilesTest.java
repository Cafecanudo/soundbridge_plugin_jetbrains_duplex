package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendFilesTest {

    @TempDir
    Path folder;

    @Test
    void foldersBecomeTheirFilesInOrderWithoutDuplicates() throws IOException {
        Path docs = Files.createDirectories(folder.resolve("docs/sub"));
        Path b = Files.writeString(folder.resolve("docs/b.txt"), "b");
        Path inner = Files.writeString(docs.resolve("c.txt"), "c");
        Path loose = Files.writeString(folder.resolve("a.txt"), "a");

        List<Path> files = SendFiles.expand(List.of(loose, folder.resolve("docs"), b));

        assertEquals(List.of(loose, b, inner), files);
    }

    @Test
    void deletionsAreKept() {
        Path gone = folder.resolve("old.txt");

        assertEquals(List.of(gone), SendFiles.expand(new SendSelection(List.of(), List.of(gone))).deletions());
    }

    @Test
    void globCharactersAreEscapedOnlyForSends() {
        assertEquals("C:/p/[[]id].ts", SendCommandBuilder.literal(Path.of("C:/p/[id].ts")).replace('\\', '/'));
        assertEquals("[[]a]-[[]b].txt", SendCommandBuilder.literal(Path.of("[a]-[b].txt")));
    }
}

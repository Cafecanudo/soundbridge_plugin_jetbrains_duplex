package io.github.cafecanudo.soundbridgeduplex.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardMemoryTest {

    @TempDir
    Path root;

    @Test
    void textBecomesTheReservedTextFile() throws IOException {
        StringSelection text = new StringSelection("olá, memória");

        assertEquals(new ClipboardMemory.Probe(ClipboardMemory.Kind.TEXT, 12), ClipboardMemory.probe(text).orElseThrow());
        Path file = ClipboardMemory.materialize(text, root).get(0);

        assertEquals(MemoryFiles.TEXT_NAME, file.getFileName().toString());
        assertEquals("olá, memória", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void copiedFilesStayAsTheyAreAndWinOverText() throws IOException {
        Path a = Files.writeString(root.resolve("a.txt"), "a");
        Transferable files = new Fixed(DataFlavor.javaFileListFlavor, List.of(a.toFile(), root.resolve("x").toFile()));

        assertEquals(new ClipboardMemory.Probe(ClipboardMemory.Kind.FILES, 1), ClipboardMemory.probe(files).orElseThrow());
        assertEquals(List.of(a), ClipboardMemory.materialize(files, root));
    }

    @Test
    void imageBecomesTheReservedPng() throws IOException {
        BufferedImage print = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        Transferable image = new Fixed(DataFlavor.imageFlavor, print);

        assertEquals(ClipboardMemory.Kind.IMAGE, ClipboardMemory.probe(image).orElseThrow().kind());
        Path file = ClipboardMemory.materialize(image, root).get(0);

        assertEquals(MemoryFiles.IMAGE_NAME, file.getFileName().toString());
        assertEquals(4, ImageIO.read(file.toFile()).getWidth());
    }

    @Test
    void emptyClipboardIsNothingAndOldFoldersAreCleaned() throws IOException {
        Path old = Files.createDirectories(root.resolve("antiga"));
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(Duration.ofDays(2))));
        Path recent = Files.createDirectories(root.resolve("recente"));

        assertTrue(ClipboardMemory.probe(new StringSelection("")).isEmpty());
        assertTrue(ClipboardMemory.probe(null).isEmpty());
        assertEquals(1, MemoryFiles.cleanup(root, Instant.now()));
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(recent));
    }

    private record Fixed(DataFlavor flavor, Object data) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{flavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor candidate) {
            return flavor.equals(candidate);
        }

        @Override
        public Object getTransferData(DataFlavor candidate) {
            return data;
        }
    }
}

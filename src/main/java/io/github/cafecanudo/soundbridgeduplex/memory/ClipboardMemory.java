package io.github.cafecanudo.soundbridgeduplex.memory;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ClipboardMemory {

    public enum Kind {
        FILES,
        IMAGE,
        TEXT
    }

    public record Probe(Kind kind, int count) {

        public String label() {
            return switch (kind) {
                case FILES -> message("memory.button.files", count);
                case IMAGE -> message("memory.button.image");
                case TEXT -> message("memory.button.text", count);
            };
        }
    }

    private ClipboardMemory() {
    }

    public static Optional<Probe> probe(Transferable contents) {
        if (contents == null) {
            return Optional.empty();
        }
        try {
            if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<Path> files = files(contents);
                return files.isEmpty() ? Optional.empty() : Optional.of(new Probe(Kind.FILES, files.size()));
            }
            if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return Optional.of(new Probe(Kind.IMAGE, 1));
            }
            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                return text == null || text.isEmpty() ? Optional.empty() : Optional.of(new Probe(Kind.TEXT, text.length()));
            }
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static List<Path> materialize(Transferable contents, Path root) throws IOException {
        Probe probe = probe(contents).orElseThrow(() -> new IOException(message("memory.empty")));
        try {
            return switch (probe.kind()) {
                case FILES -> files(contents);
                case IMAGE -> List.of(MemoryFiles.writeImage(root, (Image) contents.getTransferData(DataFlavor.imageFlavor)));
                case TEXT -> List.of(MemoryFiles.writeText(root, (String) contents.getTransferData(DataFlavor.stringFlavor)));
            };
        } catch (UnsupportedFlavorException | ClassCastException e) {
            throw new IOException(message("memory.empty"), e);
        }
    }

    private static List<Path> files(Transferable contents) throws UnsupportedFlavorException, IOException {
        Object data = contents.getTransferData(DataFlavor.javaFileListFlavor);
        if (!(data instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(File.class::isInstance)
                .map(item -> ((File) item).toPath())
                .filter(Files::exists)
                .toList();
    }
}

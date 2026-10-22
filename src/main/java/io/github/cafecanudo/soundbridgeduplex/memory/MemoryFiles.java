package io.github.cafecanudo.soundbridgeduplex.memory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class MemoryFiles {

    public static final String TEXT_NAME = "sb-memoria.txt";
    public static final String IMAGE_NAME = "sb-memoria.png";
    public static final Duration MAX_AGE = Duration.ofHours(24);

    private static final String ROOT_NAME = "sb-duplex-memoria-tx";
    private static final String IMAGE_FORMAT = "png";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private MemoryFiles() {
    }

    public static Path defaultRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), ROOT_NAME);
    }

    public static Path writeText(Path root, String text) throws IOException {
        Path file = newFolder(root).resolve(TEXT_NAME);
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    public static Path writeImage(Path root, Image image) throws IOException {
        Path file = newFolder(root).resolve(IMAGE_NAME);
        if (!ImageIO.write(toBuffered(image), IMAGE_FORMAT, file.toFile())) {
            throw new IOException("sem codificador PNG para a imagem da memória");
        }
        return file;
    }

    public static int cleanup(Path root, Instant now) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        Instant limit = now.minus(MAX_AGE);
        int removed = 0;
        try (Stream<Path> folders = Files.list(root)) {
            for (Path folder : folders.filter(Files::isDirectory).toList()) {
                if (modified(folder).isBefore(limit) && delete(folder)) {
                    removed++;
                }
            }
        } catch (IOException e) {
            return removed;
        }
        return removed;
    }

    private static Path newFolder(Path root) throws IOException {
        cleanup(root, Instant.now());
        return Files.createDirectories(root.resolve(LocalDateTime.now().format(STAMP)));
    }

    private static BufferedImage toBuffered(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static Instant modified(Path folder) throws IOException {
        FileTime time = Files.getLastModifiedTime(folder);
        return time.toInstant();
    }

    private static boolean delete(Path folder) {
        try (Stream<Path> walk = Files.walk(folder)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

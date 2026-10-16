package io.github.cafecanudo.soundbridgeduplex.send;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class SentRegistry {

    public static final String DELETED = "deleted";

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 64 * 1024;

    private SentRegistry() {
    }

    public static Map<String, String> fingerprint(SendJob job) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Path item : job.items()) {
            if (job.delete()) {
                result.put(key(item), DELETED);
            } else {
                files(item).forEach(file -> state(file).ifPresent(value -> result.put(key(file), value)));
            }
        }
        return result;
    }

    public static SendSelection unsent(SendSelection selection, Map<String, String> sent) {
        List<Path> sends = selection.sends().stream()
                .filter(path -> !state(path).equals(Optional.ofNullable(sent.get(key(path)))))
                .toList();
        List<Path> deletions = selection.deletions().stream()
                .filter(path -> !DELETED.equals(sent.get(key(path))))
                .toList();
        return new SendSelection(sends, deletions);
    }

    public static Optional<String> state(Path path) {
        if (Files.isDirectory(path)) {
            return Optional.empty();
        }
        if (!Files.exists(path)) {
            return Optional.of(DELETED);
        }
        try {
            return Optional.of(hash(path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String key(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static List<Path> files(Path item) {
        if (!Files.isDirectory(item)) {
            return List.of(item);
        }
        try (Stream<Path> walk = Files.walk(item)) {
            return walk.filter(Files::isRegularFile).toList();
        } catch (IOException | UncheckedIOException e) {
            return List.of();
        }
    }

    private static String hash(Path file) throws IOException {
        MessageDigest digest = digest();
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

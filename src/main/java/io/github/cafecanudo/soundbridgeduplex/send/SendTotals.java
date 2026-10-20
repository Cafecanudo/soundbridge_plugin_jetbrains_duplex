package io.github.cafecanudo.soundbridgeduplex.send;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class SendTotals {

    public record Total(int done, int total, int failed, double fraction) {
    }

    private final long[] sizes;
    private final long totalBytes;

    public SendTotals(long[] sizes) {
        this.sizes = sizes.clone();
        this.totalBytes = LongStream.of(sizes).sum();
    }

    public static SendTotals of(List<SendJob> jobs) {
        return new SendTotals(jobs.stream().mapToLong(SendTotals::size).toArray());
    }

    public int count() {
        return sizes.length;
    }

    public Total at(int index, Double current, int failed) {
        int done = Math.min(index, sizes.length);
        double partial = current == null ? 0 : Math.max(0, Math.min(1, current));
        return new Total(done, sizes.length, failed, fraction(done, partial));
    }

    private double fraction(int done, double partial) {
        if (sizes.length == 0) {
            return 0;
        }
        if (totalBytes == 0) {
            return Math.min(1, (done + partial) / sizes.length);
        }
        long finished = LongStream.of(sizes).limit(done).sum();
        long running = done < sizes.length ? sizes[done] : 0;
        return Math.min(1, (finished + partial * running) / (double) totalBytes);
    }

    private static long size(SendJob job) {
        return job.delete() ? 0 : job.items().stream().mapToLong(SendTotals::size).sum();
    }

    private static long size(Path item) {
        try {
            if (!Files.isDirectory(item)) {
                return Files.isRegularFile(item) ? Files.size(item) : 0;
            }
            try (Stream<Path> walk = Files.walk(item)) {
                return walk.filter(Files::isRegularFile).mapToLong(SendTotals::fileSize).sum();
            }
        } catch (IOException | UncheckedIOException e) {
            return 0;
        }
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }
}

package io.github.cafecanudo.soundbridgeduplex.calibration;

import java.util.Arrays;
import java.util.Optional;

public enum TestSize {
    S2K("2k", 2L * 1024),
    S5K("5k", 5L * 1024),
    S10K("10k", 10L * 1024),
    S20K("20k", 20L * 1024),
    S50K("50k", 50L * 1024),
    S100K("100k", 100L * 1024),
    S200K("200k", 200L * 1024),
    S500K("500k", 500L * 1024),
    S1MB("1mb", 1024L * 1024),
    S5MB("5mb", 5L * 1024 * 1024),
    S10MB("10mb", 10L * 1024 * 1024);

    public static final TestSize DEFAULT = S5K;

    private final String label;
    private final long bytes;

    TestSize(String label, long bytes) {
        this.label = label;
        this.bytes = bytes;
    }

    public String label() {
        return label;
    }

    public long bytes() {
        return bytes;
    }

    public static Optional<TestSize> ofLabel(String label) {
        return Arrays.stream(values()).filter(size -> size.label.equalsIgnoreCase(label.strip())).findFirst();
    }

    public static String labelOf(long bytes) {
        return Arrays.stream(values())
                .filter(size -> size.bytes == bytes)
                .map(TestSize::label)
                .findFirst()
                .orElse(bytes + " B");
    }
}

package io.github.cafecanudo.soundbridgeduplex.receive;

import java.util.Objects;

public record ReceiveOptions(boolean bidirectional, String directory, boolean serve, boolean copyMemory) {

    public ReceiveOptions {
        directory = Objects.requireNonNullElse(directory, "").strip();
    }

    public ReceiveOptions(boolean bidirectional, String directory, boolean serve) {
        this(bidirectional, directory, serve, false);
    }

    public ReceiveOptions(boolean bidirectional, String directory) {
        this(bidirectional, directory, false, false);
    }
}

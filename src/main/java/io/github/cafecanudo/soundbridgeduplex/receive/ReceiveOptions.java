package io.github.cafecanudo.soundbridgeduplex.receive;

import java.util.Objects;

public record ReceiveOptions(boolean bidirectional, String directory) {

    public ReceiveOptions {
        directory = Objects.requireNonNullElse(directory, "").strip();
    }
}
